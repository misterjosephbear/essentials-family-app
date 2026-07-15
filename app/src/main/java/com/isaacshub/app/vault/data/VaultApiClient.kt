package com.isaacshub.app.vault.data

import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

/** Chunk size for resumable uploads - small enough that a dropped connection only costs a few seconds of re-send. */
private const val UPLOAD_CHUNK_BYTES = 8L * 1024 * 1024

/** How many times a single chunk is retried before the caller (a WorkManager job) is told to try again later. */
private const val MAX_CHUNK_RETRIES = 3

/**
 * Talks to an isaacs-hub-storage server instance to check pairing and push files into the vault.
 *
 * Every call tries [VaultConnection.baseUrl] and [VaultConnection.remoteBaseUrl] (if set) in turn,
 * so the app keeps working whether the phone is on the same network as the server or away from it
 * (e.g. reaching it through a playit.plus tunnel instead of a LAN address) - without this, a phone
 * paired while on the home network would simply have no way to sync/back up once it left. [preferredBaseUrl]
 * (typically whichever URL answered last time, from [VaultPreferencesRepository]) is tried first so
 * repeated calls don't pay a connect-timeout tax probing a URL that's known not to be reachable
 * right now; after a call, [resolvedBaseUrl] reports which URL actually answered so the caller can
 * persist it as the new preference.
 */
class VaultApiClient(private val connection: VaultConnection, preferredBaseUrl: String? = null) {

    var resolvedBaseUrl: String? = null
        private set

    private val candidateBaseUrls: List<String> = run {
        val all = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()
        val preferred = preferredBaseUrl?.takeIf { it in all }
        if (preferred != null) listOf(preferred) + all.filterNot { it == preferred } else all
    }

    /**
     * Tries [action] against each candidate base URL in order, returning the first one that
     * completes without throwing (whether it reports business-level success or failure - only a
     * connectivity-level exception, meaning that URL couldn't be reached at all, moves on to the
     * next candidate). Returns null only if every candidate was unreachable.
     */
    private inline fun <T> tryEachBaseUrl(action: (baseUrl: String) -> T): T? {
        for (baseUrl in candidateBaseUrls) {
            val result = runCatching { action(baseUrl) }
            if (result.isSuccess) {
                resolvedBaseUrl = baseUrl
                return result.getOrThrow()
            }
        }
        return null
    }

    private fun contentUrl(baseUrl: String, remotePath: String): URL {
        val encoded = URLEncoder.encode(remotePath, "UTF-8").replace("+", "%20")
        return URL("$baseUrl/api/files/content?path=$encoded")
    }

    /** Confirms at least one configured base URL + API key actually authenticates against a running server. */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        tryEachBaseUrl { baseUrl ->
            val conn = URL("$baseUrl/api/settings/storage-root").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
            conn.connectTimeout = 6_000
            conn.readTimeout = 10_000
            try {
                conn.responseCode == HttpURLConnection.HTTP_OK
            } finally {
                conn.disconnect()
            }
        } ?: false
    }

    /** Bytes of [remotePath] the server already has for a resumable upload in progress - 0 if there's none pending. */
    private fun uploadOffsetAt(baseUrl: String, remotePath: String): Long {
        val encoded = URLEncoder.encode(remotePath, "UTF-8").replace("+", "%20")
        val conn = URL("$baseUrl/api/files/upload-offset?path=$encoded").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.connectTimeout = 6_000
        conn.readTimeout = 10_000
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return 0L
            val body = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            body.optLong("offset", 0L)
        } finally {
            conn.disconnect()
        }
    }

    private fun putChunk(
        baseUrl: String,
        remotePath: String,
        mimeType: String,
        raf: RandomAccessFile,
        start: Long,
        end: Long,
        total: Long
    ) {
        val conn = contentUrl(baseUrl, remotePath).openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", mimeType)
        conn.setRequestProperty("Content-Range", "bytes $start-${end - 1}/$total")
        conn.setFixedLengthStreamingMode(end - start)
        conn.connectTimeout = 6_000
        conn.readTimeout = 60_000
        try {
            raf.seek(start)
            val buffer = ByteArray(64 * 1024)
            var remaining = end - start
            conn.outputStream.use { output ->
                while (remaining > 0) {
                    val read = raf.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
            check(conn.responseCode in 200..299) { "Upload chunk failed (${conn.responseCode})" }
        } finally {
            conn.disconnect()
        }
    }

    /** Whole-body PUT with no Content-Range header - used only for empty files, which have no bytes to chunk or resume. */
    private fun putEmptyFile(baseUrl: String, remotePath: String, mimeType: String) {
        val conn = contentUrl(baseUrl, remotePath).openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", mimeType)
        conn.setFixedLengthStreamingMode(0L)
        conn.connectTimeout = 6_000
        conn.readTimeout = 60_000
        try {
            conn.outputStream.close()
            check(conn.responseCode in 200..299) { "Upload failed (${conn.responseCode})" }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Uploads [file]'s bytes to [remotePath] in the vault, creating any missing parent folders.
     * Resumes from whatever byte offset the server already has (querying it fresh each call), so a
     * transfer interrupted mid-file - a dropped connection, the app getting killed, a batch that
     * timed out - picks up where it left off next time this is called instead of re-sending bytes
     * the server already has. A chunk that fails outright (after its own retries) makes this whole
     * call return false; the caller (a WorkManager job) is expected to retry the call later, which
     * will resume correctly since the offset is tracked server-side, not in memory here.
     */
    suspend fun uploadFile(file: File, remotePath: String, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        val uploaded = tryEachBaseUrl { baseUrl ->
            if (file.length() == 0L) {
                putEmptyFile(baseUrl, remotePath, mimeType)
                return@tryEachBaseUrl true
            }

            var offset = uploadOffsetAt(baseUrl, remotePath)
            if (offset > file.length()) offset = 0L // Stale/mismatched partial upload from a different file at this path - start over.

            RandomAccessFile(file, "r").use { raf ->
                while (offset < file.length()) {
                    val end = minOf(offset + UPLOAD_CHUNK_BYTES, file.length())
                    var attempt = 0
                    while (true) {
                        try {
                            putChunk(baseUrl, remotePath, mimeType, raf, offset, end, file.length())
                            break
                        } catch (e: Exception) {
                            attempt++
                            if (attempt > MAX_CHUNK_RETRIES) throw e
                            Thread.sleep(500L * attempt)
                        }
                    }
                    offset = end
                }
            }
            true
        }
        uploaded ?: false
    }
}

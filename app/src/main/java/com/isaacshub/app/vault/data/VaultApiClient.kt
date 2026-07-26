package com.isaacshub.app.vault.data

import com.isaacshub.app.core.network.MultiUrlApiClient
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
 * Extends [MultiUrlApiClient] to handle automatic fallback between local and remote URLs.
 */
class VaultApiClient(
    connection: VaultConnection,
    preferredBaseUrl: String? = null
) : MultiUrlApiClient(connection, preferredBaseUrl) {

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

    /**
     * Downloads a file from [remotePath] in the vault to [destinationFile].
     * Returns true if successful, false if the file doesn't exist or download failed.
     */
    suspend fun downloadFile(remotePath: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        val downloaded = tryEachBaseUrl { baseUrl ->
            val conn = contentUrl(baseUrl, remotePath).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
            conn.connectTimeout = 6_000
            conn.readTimeout = 60_000
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    return@tryEachBaseUrl false
                }

                // Download to temp file first, then move on success
                val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")
                tempFile.parentFile?.mkdirs()

                conn.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Move temp file to final destination
                tempFile.renameTo(destinationFile)
                true
            } finally {
                conn.disconnect()
            }
        }
        downloaded ?: false
    }
}

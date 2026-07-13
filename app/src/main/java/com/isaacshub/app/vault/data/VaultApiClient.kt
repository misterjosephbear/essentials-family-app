package com.isaacshub.app.vault.data

import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Talks to an isaacs-hub-storage server instance to check pairing and push files into the vault. */
class VaultApiClient(private val connection: VaultConnection) {

    private fun contentUrl(remotePath: String): URL {
        val encoded = URLEncoder.encode(remotePath, "UTF-8").replace("+", "%20")
        return URL("${connection.baseUrl}/api/files/content?path=$encoded")
    }

    /** Confirms the base URL + API key actually authenticate against a running server. */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("${connection.baseUrl}/api/settings/storage-root")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                conn.responseCode == HttpURLConnection.HTTP_OK
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
    }

    /** Uploads [file]'s bytes to [remotePath] in the vault, creating any missing parent folders. */
    suspend fun uploadFile(file: File, remotePath: String, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = contentUrl(remotePath).openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
            conn.setRequestProperty("Content-Type", mimeType)
            conn.setFixedLengthStreamingMode(file.length())
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            try {
                file.inputStream().use { input -> conn.outputStream.use { output -> input.copyTo(output) } }
                conn.responseCode in 200..299
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
    }
}

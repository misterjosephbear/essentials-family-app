package com.isaacshub.app.core.network

import com.isaacshub.app.vault.domain.VaultConnection
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Base class for API clients that talk to isaacs-hub-server.
 * Provides common HTTP functionality:
 * - Automatic fallback between baseUrl and remoteBaseUrl (via MultiUrlApiClient)
 * - Consistent timeout values
 * - Error message parsing
 * - Authorization header setup
 */
abstract class BaseApiClient(
    connection: VaultConnection,
    preferredBaseUrl: String? = null
) : MultiUrlApiClient(connection, preferredBaseUrl) {

    /**
     * Opens an HTTP connection to [path] at [baseUrl] with common defaults:
     * - Authorization header with API key
     * - 6s connect timeout, 10s read timeout
     */
    protected fun openConnection(baseUrl: String, path: String): HttpURLConnection {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.connectTimeout = 6_000
        conn.readTimeout = 10_000
        return conn
    }

    /**
     * Parses error message from connection's error stream, falling back to generic message.
     */
    protected fun errorMessageFrom(conn: HttpURLConnection): String {
        val body = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
        val parsed = body?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }?.takeIf { it.isNotBlank() }
        return parsed ?: "Request failed (HTTP ${conn.responseCode})"
    }

    /**
     * Performs a GET request to [path], returning the response body as a JSON object.
     * Throws if the request fails.
     */
    protected fun get(baseUrl: String, path: String): JSONObject {
        val conn = openConnection(baseUrl, path)
        return try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body)
            } else {
                throw Exception(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Performs a POST request to [path] with optional JSON [body].
     * Returns the response body as a JSON object. Throws if the request fails.
     */
    protected fun post(baseUrl: String, path: String, body: JSONObject? = null): JSONObject {
        val conn = openConnection(baseUrl, path)
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = body != null
        return try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(responseBody)
            } else {
                throw Exception(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Performs a PUT request to [path] with optional JSON [body].
     * Returns the response body as a JSON object. Throws if the request fails.
     */
    protected fun put(baseUrl: String, path: String, body: JSONObject? = null): JSONObject {
        val conn = openConnection(baseUrl, path)
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = body != null
        return try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(responseBody)
            } else {
                throw Exception(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Performs a DELETE request to [path].
     * Returns true if successful (HTTP 200-299). Throws if the request fails.
     */
    protected fun delete(baseUrl: String, path: String): Boolean {
        val conn = openConnection(baseUrl, path)
        conn.requestMethod = "DELETE"
        return try {
            conn.responseCode in 200..299
        } finally {
            conn.disconnect()
        }
    }
}

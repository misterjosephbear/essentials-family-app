package com.isaacshub.app.debug

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Debug logging system that can send app logs to the isaacs-hub-server.
 * Captures logcat output filtered by app tag and posts it to the server's debug endpoint.
 */
object DebugLogger {
    private const val TAG = "DebugLogger"
    private const val SERVER_URL = "http://brownserver2.local:4000/api/debug/logs"

    /**
     * Captures recent logcat entries for this app and sends them to the server.
     * Filters for relevant tags: RoutePlayerViewModel, RouteDirectionsFetcher, MailScanViewModel, etc.
     */
    suspend fun sendLogsToServer(context: Context, label: String = "Debug Logs"): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the app's package name to filter logs
            val packageName = context.packageName

            // Capture logcat with filters for our app
            val process = Runtime.getRuntime().exec(arrayOf(
                "logcat",
                "-d",  // Dump mode (don't follow)
                "-v", "time",  // Include timestamps
                "RoutePlayerViewModel:D",
                "RouteDirectionsFetcher:D",
                "MailScanViewModel:D",
                "MailScanParser:D",
                "*:S"  // Silence everything else
            ))

            val logs = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }

            process.waitFor()

            if (logs.isEmpty()) {
                Log.w(TAG, "No logs captured")
                return@withContext false
            }

            // Send logs to server
            val url = URL(SERVER_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = """
                {
                    "label": "$label",
                    "timestamp": ${System.currentTimeMillis()},
                    "logs": ${org.json.JSONObject.quote(logs)}
                }
            """.trimIndent()

            conn.outputStream.use { it.write(json.toByteArray()) }

            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
            }

            Log.d(TAG, "Logs sent to server, response: $responseCode, body: $responseBody")

            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send logs to server: ${e.message}", e)
            false
        }
    }

    /**
     * Captures a snapshot of the current route player state and sends it to the server.
     */
    suspend fun sendRouteDebugInfo(
        routeId: Long?,
        stopCount: Int,
        roadRoutePointCount: Int?,
        debugInfo: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(SERVER_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = """
                {
                    "label": "Route Debug Snapshot",
                    "timestamp": ${System.currentTimeMillis()},
                    "routeId": $routeId,
                    "stopCount": $stopCount,
                    "roadRoutePointCount": $roadRoutePointCount,
                    "debugInfo": ${org.json.JSONObject.quote(debugInfo)}
                }
            """.trimIndent()

            conn.outputStream.use { it.write(json.toByteArray()) }

            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
            }

            Log.d(TAG, "Route debug info sent, response: $responseCode, body: $responseBody")

            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send route debug info: ${e.message}", e)
            false
        }
    }
}

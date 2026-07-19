package com.isaacshub.app.settings.data

import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

sealed interface DebugTriggerResult {
    /** Server returned 202 - the discord-bridge process is running the debug session now. */
    data object Started : DebugTriggerResult

    /** Server returned 409 - a debug session was already in progress. */
    data object AlreadyRunning : DebugTriggerResult

    data class Failed(val message: String) : DebugTriggerResult
}

/** Talks to an isaacs-hub-storage server instance's general (non-vault) settings endpoints. */
class SettingsApiClient(private val connection: VaultConnection) {

    /** Kicks off the server's unattended discord-bridge self-debug session - see server's POST /api/settings/discord-bridge/debug. */
    suspend fun triggerDiscordBridgeDebug(): DebugTriggerResult = withContext(Dispatchers.IO) {
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()
        for (baseUrl in candidates) {
            val result = runCatching { postDebugTrigger(baseUrl) }
            result.getOrNull()?.let { return@withContext it }
        }
        DebugTriggerResult.Failed("Couldn't reach the server.")
    }

    private fun postDebugTrigger(baseUrl: String): DebugTriggerResult {
        val conn = URL("$baseUrl/api/settings/discord-bridge/debug").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.doOutput = false
        conn.connectTimeout = 6_000
        conn.readTimeout = 10_000
        return try {
            val code = conn.responseCode
            when (code) {
                202 -> DebugTriggerResult.Started
                409 -> DebugTriggerResult.AlreadyRunning
                else -> DebugTriggerResult.Failed(errorMessageFrom(conn, code))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessageFrom(conn: HttpURLConnection, code: Int): String {
        val body = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
        val parsed = body?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }?.takeIf { it.isNotBlank() }
        return parsed ?: "Request failed (HTTP $code)"
    }
}

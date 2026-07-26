package com.isaacshub.app.settings.data

import com.isaacshub.app.core.network.BaseApiClient
import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

sealed interface DebugTriggerResult {
    /** Server returned 202 - the discord-bridge process is running the debug session now. */
    data object Started : DebugTriggerResult

    /** Server returned 409 - a debug session was already in progress. */
    data object AlreadyRunning : DebugTriggerResult

    data class Failed(val message: String) : DebugTriggerResult
}

/** Talks to an isaacs-hub-storage server instance's general (non-vault) settings endpoints. */
class SettingsApiClient(connection: VaultConnection) : BaseApiClient(connection) {

    /** Kicks off the server's unattended discord-bridge self-debug session - see server's POST /api/settings/discord-bridge/debug. */
    suspend fun triggerDiscordBridgeDebug(): DebugTriggerResult = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            postDebugTrigger(baseUrl)
        }
        result ?: DebugTriggerResult.Failed("Couldn't reach the server.")
    }

    private fun postDebugTrigger(baseUrl: String): DebugTriggerResult {
        val conn = openConnection(baseUrl, "/api/settings/discord-bridge/debug")
        conn.requestMethod = "POST"
        conn.doOutput = false
        return try {
            val code = conn.responseCode
            when (code) {
                202 -> DebugTriggerResult.Started
                409 -> DebugTriggerResult.AlreadyRunning
                else -> DebugTriggerResult.Failed(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }
}

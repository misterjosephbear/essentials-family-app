package com.isaacshub.app.featurefunnel.data

import com.isaacshub.app.core.network.BaseApiClient
import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

sealed interface SendPromptResult {
    data object Success : SendPromptResult
    data object LimitHit : SendPromptResult  // Claude usage limits reached
    data class Failed(val message: String) : SendPromptResult
}

class FeatureFunnelApiClient(connection: VaultConnection) : BaseApiClient(connection) {

    /**
     * Sends a feature prompt to the Discord channel configured for Claude Code.
     * The server endpoint will format and post the message to Discord.
     */
    suspend fun sendPromptToDiscord(
        channelId: String,
        promptText: String
    ): SendPromptResult = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val requestBody = JSONObject().apply {
                put("channelId", channelId)
                put("promptText", promptText)
            }
            val conn = openConnection(baseUrl, "/api/feature-funnel/send-prompt")
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.readTimeout = 30_000  // Longer timeout for Discord API

            try {
                conn.outputStream.bufferedWriter().use { it.write(requestBody.toString()) }
                val code = conn.responseCode
                when (code) {
                    200 -> SendPromptResult.Success
                    429 -> SendPromptResult.LimitHit
                    else -> SendPromptResult.Failed(errorMessageFrom(conn))
                }
            } finally {
                conn.disconnect()
            }
        }
        result ?: SendPromptResult.Failed("Couldn't reach the server.")
    }

    /**
     * Fetches the latest git commit hash from the isaacs-hub-server repo.
     * Used for completion detection.
     */
    suspend fun getLatestCommitHash(): String? = withContext(Dispatchers.IO) {
        tryEachBaseUrl { baseUrl ->
            val json = get(baseUrl, "/api/feature-funnel/latest-commit")
            json.optString("hash", null)
        }
    }

    /**
     * Sends a Discord notification that a feature was completed.
     * Pings the user with the specified user ID.
     */
    suspend fun notifyCompletion(
        channelId: String,
        promptTitle: String,
        commitHash: String
    ): Boolean = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val requestBody = JSONObject().apply {
                put("channelId", channelId)
                put("promptTitle", promptTitle)
                put("commitHash", commitHash)
            }
            post(baseUrl, "/api/feature-funnel/notify-completion", requestBody)
            true
        }
        result == true
    }
}

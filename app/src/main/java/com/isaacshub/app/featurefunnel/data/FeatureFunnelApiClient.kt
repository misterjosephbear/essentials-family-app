package com.isaacshub.app.featurefunnel.data

import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface SendPromptResult {
    data object Success : SendPromptResult
    data object LimitHit : SendPromptResult  // Claude usage limits reached
    data class Failed(val message: String) : SendPromptResult
}

class FeatureFunnelApiClient(private val connection: VaultConnection) {

    /**
     * Sends a feature prompt to the Discord channel configured for Claude Code.
     * The server endpoint will format and post the message to Discord.
     */
    suspend fun sendPromptToDiscord(
        channelId: String,
        promptText: String
    ): SendPromptResult = withContext(Dispatchers.IO) {
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()

        for (baseUrl in candidates) {
            val result = runCatching { postPrompt(baseUrl, channelId, promptText) }
            result.getOrNull()?.let { return@withContext it }
        }

        SendPromptResult.Failed("Couldn't reach the server.")
    }

    private fun postPrompt(baseUrl: String, channelId: String, promptText: String): SendPromptResult {
        val conn = URL("$baseUrl/api/feature-funnel/send-prompt").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 6_000
        conn.readTimeout = 30_000

        return try {
            // Send request body
            val requestBody = JSONObject().apply {
                put("channelId", channelId)
                put("promptText", promptText)
            }
            conn.outputStream.bufferedWriter().use { it.write(requestBody.toString()) }

            val code = conn.responseCode
            when (code) {
                200 -> SendPromptResult.Success
                429 -> SendPromptResult.LimitHit  // Rate limit / usage limit
                else -> SendPromptResult.Failed(errorMessageFrom(conn, code))
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Fetches the latest git commit hash from the isaacs-hub-server repo.
     * Used for completion detection.
     */
    suspend fun getLatestCommitHash(): String? = withContext(Dispatchers.IO) {
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()

        for (baseUrl in candidates) {
            val result = runCatching { fetchCommitHash(baseUrl) }
            result.getOrNull()?.let { return@withContext it }
        }

        null
    }

    private fun fetchCommitHash(baseUrl: String): String? {
        val conn = URL("$baseUrl/api/feature-funnel/latest-commit").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.connectTimeout = 6_000
        conn.readTimeout = 10_000

        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            body.optString("hash", null)
        } finally {
            conn.disconnect()
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
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()

        for (baseUrl in candidates) {
            val result = runCatching {
                postNotification(baseUrl, channelId, promptTitle, commitHash)
            }
            if (result.getOrNull() == true) return@withContext true
        }

        false
    }

    private fun postNotification(
        baseUrl: String,
        channelId: String,
        promptTitle: String,
        commitHash: String
    ): Boolean {
        val conn = URL("$baseUrl/api/feature-funnel/notify-completion").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 6_000
        conn.readTimeout = 10_000

        return try {
            val requestBody = JSONObject().apply {
                put("channelId", channelId)
                put("promptTitle", promptTitle)
                put("commitHash", commitHash)
            }
            conn.outputStream.bufferedWriter().use { it.write(requestBody.toString()) }

            conn.responseCode in 200..299
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessageFrom(conn: HttpURLConnection, code: Int): String {
        val body = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
        val parsed = body?.let {
            runCatching { JSONObject(it).optString("error") }.getOrNull()
        }?.takeIf { it.isNotBlank() }
        return parsed ?: "Request failed (HTTP $code)"
    }
}

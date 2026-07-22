package com.isaacshub.app.discord

import android.content.Context
import android.util.Log
import com.isaacshub.app.vault.data.VaultPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Discord Rich Presence Manager that communicates with the isaacs-hub-server.
 *
 * The server's discord-bridge process manages the actual Discord bot presence,
 * allowing 24/7 Rich Presence updates even when the app is closed.
 */
class DiscordRichPresenceManager private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val vaultPrefs = VaultPreferencesRepository(context)

    companion object {
        private const val TAG = "DiscordRPC"

        @Volatile
        private var instance: DiscordRichPresenceManager? = null

        fun getInstance(context: Context): DiscordRichPresenceManager {
            return instance ?: synchronized(this) {
                instance ?: DiscordRichPresenceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setSleepingPresence(sleepStartTime: Instant, wakeTime: Instant) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("type", "sleeping")
                    put("sleepStartEpochMs", sleepStartTime.toEpochMilli())
                    put("wakeTimeEpochMs", wakeTime.toEpochMilli())
                }

                sendPresenceUpdate(payload)

                Log.d(TAG, "Updated Discord status to sleeping")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set sleeping presence", e)
            }
        }
    }

    fun setDeliveringMailPresence(
        currentStop: Int,
        totalStops: Int,
        estimatedCompletionTime: Instant?
    ) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("type", "delivering_mail")
                    put("currentStop", currentStop)
                    put("totalStops", totalStops)
                    if (estimatedCompletionTime != null) {
                        put("estimatedCompletionEpochMs", estimatedCompletionTime.toEpochMilli())
                    }
                }

                sendPresenceUpdate(payload)

                Log.d(TAG, "Updated Discord status to delivering mail: $currentStop/$totalStops")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set delivering mail presence", e)
            }
        }
    }

    fun clearPresence() {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("type", "idle")
                }

                sendPresenceUpdate(payload)

                Log.d(TAG, "Cleared Discord presence")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear presence", e)
            }
        }
    }

    private suspend fun sendPresenceUpdate(payload: JSONObject) {
        val connection = vaultPrefs.connection.first() ?: run {
            Log.w(TAG, "No vault connection configured, skipping Discord update")
            return
        }

        val baseUrl = connection.baseUrl
        val apiKey = connection.apiKey

        val url = URL("$baseUrl/api/rich-presence/presence")
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Server returned error $responseCode: $errorBody")
            }
        } finally {
            conn.disconnect()
        }
    }
}

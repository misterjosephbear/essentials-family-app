package com.isaacshub.essentials.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthRepository(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "essentials_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthToken> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$serverUrl/api/essentials/auth/child-login")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val requestBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            conn.outputStream.use { it.write(requestBody.toString().toByteArray()) }

            if (conn.responseCode != 200) {
                throw Exception("Login failed: ${conn.responseMessage}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val authToken = AuthToken(
                token = json.getString("token"),
                userId = json.getLong("userId"),
                username = json.getString("username"),
                displayName = json.getString("displayName")
            )

            // Save auth token
            saveAuthToken(authToken, serverUrl)

            authToken
        }
    }

    fun saveAuthToken(token: AuthToken, serverUrl: String) {
        encryptedPrefs.edit().apply {
            putString("auth_token", token.token)
            putLong("user_id", token.userId)
            putString("username", token.username)
            putString("display_name", token.displayName)
            putString("server_url", serverUrl)
            apply()
        }
    }

    fun getAuthToken(): AuthToken? {
        val token = encryptedPrefs.getString("auth_token", null) ?: return null
        val userId = encryptedPrefs.getLong("user_id", -1)
        val username = encryptedPrefs.getString("username", null) ?: return null
        val displayName = encryptedPrefs.getString("display_name", null) ?: return null

        return AuthToken(token, userId, username, displayName)
    }

    fun getServerUrl(): String? {
        return encryptedPrefs.getString("server_url", null)
    }

    fun clearAuthToken() {
        encryptedPrefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getAuthToken() != null
}

data class AuthToken(
    val token: String,
    val userId: Long,
    val username: String,
    val displayName: String
)

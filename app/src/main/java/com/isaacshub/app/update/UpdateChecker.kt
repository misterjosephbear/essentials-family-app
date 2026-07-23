package com.isaacshub.app.update

import com.isaacshub.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionCode: Long,
    val versionName: String,
    val assetId: Long
)

sealed interface UpdateCheckResult {
    data class Success(val release: ReleaseInfo) : UpdateCheckResult
    data class Failure(val reason: String) : UpdateCheckResult
}

/**
 * Checks the web server for a newer build by fetching version.json. This replaced the GitHub
 * Releases approach because GitHub CDN had reliability issues and we're deploying directly
 * to the web server anyway.
 *
 * The version.json file is deployed alongside the APK and contains versionCode, versionName,
 * and downloadUrl. No authentication is needed since it's a public web server.
 */
object UpdateChecker {
    private const val VERSION_JSON_URL = "http://isaacs-hub.playit.plus/version.json"

    suspend fun fetchLatestRelease(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(VERSION_JSON_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            try {
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    UpdateCheckResult.Failure("Version check returned HTTP $code: ${errorBody.take(200)}")
                } else {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseVersionJson(body)?.let { UpdateCheckResult.Success(it) }
                        ?: UpdateCheckResult.Failure("version.json is missing required fields")
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failure("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun parseVersionJson(body: String): ReleaseInfo? {
        return try {
            val json = JSONObject(body)
            val versionCode = json.optLong("versionCode", -1L)
            val versionName = json.optString("versionName", "")
            if (versionCode > 0 && versionName.isNotEmpty()) {
                // assetId is not used anymore but kept for compatibility
                ReleaseInfo(versionCode, versionName, assetId = 0)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

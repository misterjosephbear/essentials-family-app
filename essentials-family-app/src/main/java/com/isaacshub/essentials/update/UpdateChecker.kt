package com.isaacshub.essentials.update

import com.isaacshub.essentials.BuildConfig
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
 * Checks GitHub Releases for a newer build of Essentials Family app.
 */
object UpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/latest"

    suspend fun fetchLatestRelease(): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (BuildConfig.UPDATE_CHECK_TOKEN.isBlank()) {
            return@withContext UpdateCheckResult.Failure("no update-check token embedded in this build")
        }
        try {
            val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_CHECK_TOKEN}")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            try {
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    UpdateCheckResult.Failure("GitHub API returned HTTP $code: ${errorBody.take(200)}")
                } else {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseRelease(body)?.let { UpdateCheckResult.Success(it) }
                        ?: UpdateCheckResult.Failure("latest release has no .apk asset")
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failure("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun parseRelease(body: String): ReleaseInfo? {
        val json = JSONObject(body)
        val tagName = json.optString("tag_name")
        val versionCode = tagName.removePrefix("v").toLongOrNull() ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            // Look specifically for essentials-family-app APK
            if (name.contains("essentials-family") && name.endsWith(".apk")) {
                val assetId = asset.optLong("id", -1L)
                if (assetId > 0) {
                    return ReleaseInfo(versionCode, json.optString("name", tagName), assetId)
                }
            }
        }
        return null
    }
}

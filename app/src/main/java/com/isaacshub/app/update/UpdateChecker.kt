package com.isaacshub.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String
)

/**
 * Checks GitHub Releases for a newer build. CI (.github/workflows/release.yml) tags every release
 * "v<versionCode>" and attaches the signed APK as an asset, so the numeric versionCode in the tag
 * name can be compared directly against what's installed without parsing the APK itself.
 */
object UpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/latest"

    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseRelease(body)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun parseRelease(body: String): ReleaseInfo? {
        val json = JSONObject(body)
        val tagName = json.optString("tag_name")
        val versionCode = tagName.removePrefix("v").toLongOrNull() ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            if (name.endsWith(".apk")) {
                val downloadUrl = asset.optString("browser_download_url")
                if (downloadUrl.isNotBlank()) {
                    return ReleaseInfo(versionCode, json.optString("name", tagName), downloadUrl)
                }
            }
        }
        return null
    }
}

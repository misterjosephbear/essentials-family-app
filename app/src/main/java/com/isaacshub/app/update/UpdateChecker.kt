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

/**
 * Checks GitHub Releases for a newer build. CI (.github/workflows/release.yml) tags every release
 * "v<versionCode>" and attaches the signed APK as an asset, so the numeric versionCode in the tag
 * name can be compared directly against what's installed without parsing the APK itself.
 *
 * isaacs-hub is a private repo, so every request needs UPDATE_CHECK_TOKEN - a fine-grained PAT
 * scoped to read-only Contents access on just this repo, baked in by CI via BuildConfig. Local/debug
 * builds have no token and simply never find an update.
 */
object UpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/latest"

    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        if (BuildConfig.UPDATE_CHECK_TOKEN.isBlank()) return@withContext null
        runCatching {
            val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_CHECK_TOKEN}")
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
                val assetId = asset.optLong("id", -1L)
                if (assetId > 0) {
                    return ReleaseInfo(versionCode, json.optString("name", tagName), assetId)
                }
            }
        }
        return null
    }
}

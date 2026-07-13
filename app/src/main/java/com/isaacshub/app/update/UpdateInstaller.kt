package com.isaacshub.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.isaacshub.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateInstaller {
    private const val ASSET_URL_TEMPLATE =
        "https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/assets/%d"

    /** Downloads the APK asset for [release] to cache, reporting 0f..1f via [onProgress], then launches the installer. */
    suspend fun downloadAndInstall(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float) -> Unit
    ) {
        val file = withContext(Dispatchers.IO) {
            var connection = openAssetConnection(ASSET_URL_TEMPLATE.format(release.assetId))
            try {
                connection.connect()

                // Private-repo asset downloads redirect to a temporary pre-signed URL that must be
                // fetched with no Authorization header of its own - HttpURLConnection's built-in
                // redirect following would resend ours, which the signed URL's host rejects.
                if (connection.responseCode in 300..399) {
                    val redirectUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    connection = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 15_000
                        connect()
                    }
                }

                val total = connection.contentLength
                val target = File(context.cacheDir, "update.apk")
                connection.inputStream.use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress(downloaded.toFloat() / total)
                        }
                    }
                }
                target
            } finally {
                connection.disconnect()
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openAssetConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_CHECK_TOKEN}")
        }
}

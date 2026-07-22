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
    // Direct download from web server instead of GitHub releases (GitHub CDN has known issues)
    private const val DIRECT_APK_URL = "http://isaacs-hub.playit.plus/isaacs-hub-release.apk"

    /** Downloads the APK from web server to cache, reporting 0f..1f via [onProgress], then launches the installer. */
    suspend fun downloadAndInstall(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float) -> Unit
    ) {
        val file = withContext(Dispatchers.IO) {
            val connection = (URL(DIRECT_APK_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                connection.connect()

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
}

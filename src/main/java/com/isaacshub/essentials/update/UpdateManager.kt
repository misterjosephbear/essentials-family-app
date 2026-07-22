package com.isaacshub.essentials.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

/**
 * Manages app updates by checking server for new versions
 * and downloading/installing APK files.
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
    }

    /**
     * Check if an update is available
     */
    suspend fun checkForUpdate(serverUrl: String, authToken: String): Result<AppVersion?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$serverUrl/api/essentials/app/latest-version")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $authToken")
            conn.setRequestProperty("Content-Type", "application/json")

            if (conn.responseCode != 200) {
                throw Exception("Failed to check for updates: HTTP ${conn.responseCode}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val latestVersion = AppVersion(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                downloadUrl = json.getString("downloadUrl"),
                releaseNotes = json.optString("releaseNotes", "")
            )

            val currentVersionCode = getCurrentVersionCode()

            Log.d(TAG, "Current version: $currentVersionCode, Latest version: ${latestVersion.versionCode}")

            if (latestVersion.versionCode > currentVersionCode) {
                latestVersion
            } else {
                null // No update available
            }
        }
    }

    /**
     * Download and install an update
     */
    fun downloadAndInstallUpdate(updateUrl: String, versionName: String) {
        val fileName = "essentials-family-$versionName.apk"
        val request = DownloadManager.Request(Uri.parse(updateUrl)).apply {
            setTitle("Essentials Family Update")
            setDescription("Downloading version $versionName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        Log.d(TAG, "Download started: $downloadId")

        // Register receiver to install APK when download completes
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(downloadManager, downloadId, fileName)
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(downloadManager: DownloadManager, downloadId: Long, fileName: String) {
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                    val apkFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    if (apkFile.exists()) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

                        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                apkFile
                            )
                        } else {
                            Uri.fromFile(apkFile)
                        }

                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")

                        context.startActivity(intent)

                        Log.d(TAG, "APK installation started")
                    } else {
                        Log.e(TAG, "Downloaded APK file not found")
                    }
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version code", e)
            0
        }
    }
}

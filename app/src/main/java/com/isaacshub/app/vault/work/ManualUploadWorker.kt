package com.isaacshub.app.vault.work

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.isaacshub.app.App
import com.isaacshub.app.vault.data.VaultApiClient
import kotlinx.coroutines.flow.first
import java.io.File

const val PROGRESS_DISPLAY_NAME = "display_name"
const val KEY_URI = "uri"
const val KEY_DISPLAY_NAME = "display_name"
const val KEY_MIME_TYPE = "mime_type"
const val KEY_TARGET_PATH = "target_path"
const val MANUAL_UPLOAD_TAG = "manual-upload"

/**
 * Uploads a single user-picked file (via "Upload a file" in Photo Vault) to the paired server.
 *
 * Unlike the old implementation (a plain coroutine in the ViewModel), this runs as WorkManager work
 * - its input data is persisted to WorkManager's own database, so the upload survives the app being
 * closed or the process being killed mid-transfer and automatically resumes (with network-type and
 * backoff-retry constraints, same as the other vault workers) instead of silently vanishing. The
 * picked document's URI permission is taken as persistable (see `VaultHomeViewModel.uploadPickedFile`)
 * specifically so it's still readable after a process restart.
 */
class ManualUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val prefs = app.vaultPreferencesRepository
        val connection = prefs.connection.first() ?: return Result.failure()

        val displayName = inputData.getString(KEY_DISPLAY_NAME) ?: "file"
        setProgress(workDataOf(PROGRESS_DISPLAY_NAME to displayName))

        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "application/octet-stream"
        val targetPath = inputData.getString(KEY_TARGET_PATH) ?: return Result.failure()
        val uri = Uri.parse(uriString)

        val tempFile = File(applicationContext.cacheDir, "vault_manual_upload_$id")
        return try {
            val input = applicationContext.contentResolver.openInputStream(uri)
                ?: return Result.failure(workDataOf(PROGRESS_DISPLAY_NAME to displayName))
            input.use { stream -> tempFile.outputStream().use { output -> stream.copyTo(output) } }

            val client = VaultApiClient(connection, prefs.preferredBaseUrl.first())
            val uploaded = client.uploadFile(tempFile, targetPath, mimeType)
            client.resolvedBaseUrl?.let { prefs.setPreferredBaseUrl(it) }

            if (uploaded) {
                applicationContext.contentResolver.releasePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: SecurityException) {
            // Permission to the picked document is gone (e.g. revoked by its source app) - retrying won't help.
            Result.failure(workDataOf(PROGRESS_DISPLAY_NAME to displayName))
        } finally {
            tempFile.delete()
        }
    }
}

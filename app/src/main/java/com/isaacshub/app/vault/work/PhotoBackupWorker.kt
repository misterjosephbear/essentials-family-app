package com.isaacshub.app.vault.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.isaacshub.app.App
import com.isaacshub.app.vault.data.VaultApiClient
import com.isaacshub.app.vault.domain.planPendingUploads
import com.isaacshub.app.vault.media.MediaStorePhotoScanner
import kotlinx.coroutines.flow.first
import java.io.File

/** Uploads photos added since the last successful sync to the paired isaacs-hub-storage server. */
class PhotoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val prefs = app.vaultPreferencesRepository
        val connection = prefs.connection.first() ?: return Result.success()

        val scanner = MediaStorePhotoScanner(applicationContext)
        val lastSync = prefs.lastSyncEpochMillis.first()
        val pending = planPendingUploads(scanner.queryPhotos(), lastSync)
        if (pending.isEmpty()) return Result.success()

        val client = VaultApiClient(connection)
        var lastSuccessfulEpochSeconds: Long? = null
        var allSucceeded = true

        for (upload in pending) {
            val tempFile = File(applicationContext.cacheDir, "vault_upload_${upload.photo.id}")
            try {
                val input = scanner.openInputStream(upload.photo.id)
                if (input == null) {
                    // Photo disappeared (deleted) between query and upload - skip it, don't fail the batch.
                    continue
                }
                input.use { stream -> tempFile.outputStream().use { output -> stream.copyTo(output) } }
                val uploaded = client.uploadFile(tempFile, upload.remotePath, upload.photo.mimeType)
                if (!uploaded) {
                    allSucceeded = false
                    break
                }
                lastSuccessfulEpochSeconds = upload.photo.dateAddedEpochSeconds
            } finally {
                tempFile.delete()
            }
        }

        lastSuccessfulEpochSeconds?.let { prefs.setLastSyncEpochMillis(it * 1000) }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}

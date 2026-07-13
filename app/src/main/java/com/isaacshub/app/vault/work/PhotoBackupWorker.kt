package com.isaacshub.app.vault.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.isaacshub.app.App
import com.isaacshub.app.vault.data.VaultApiClient
import com.isaacshub.app.vault.domain.PendingUpload
import com.isaacshub.app.vault.domain.planPendingUploads
import com.isaacshub.app.vault.media.MediaStoreScanner
import kotlinx.coroutines.flow.first
import java.io.File

/** Cap per run so a large initial backlog can't run long enough to hit WorkManager's execution time limit and get killed mid-batch. */
private const val BATCH_SIZE = 25

const val PROGRESS_UPLOADED = "uploaded"
const val PROGRESS_REMAINING = "remaining"

/** Uploads photos added since the last successful sync to the paired isaacs-hub-storage server. */
class PhotoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val prefs = app.vaultPreferencesRepository
        val connection = prefs.connection.first() ?: return Result.success()

        val scanner = MediaStoreScanner(applicationContext)
        val lastSync = prefs.lastSyncEpochMillis.first()
        val pending = planPendingUploads(scanner.queryMedia(), lastSync)
        if (pending.isEmpty()) return Result.success()

        val batch = pending.take(BATCH_SIZE)
        val client = VaultApiClient(connection)
        var lastSuccessfulEpochSeconds: Long? = null
        var allSucceeded = true

        for ((index, upload) in batch.withIndex()) {
            setProgress(workDataOf(PROGRESS_UPLOADED to index, PROGRESS_REMAINING to pending.size - index))
            if (!uploadOne(scanner, client, upload)) {
                allSucceeded = false
                break
            }
            lastSuccessfulEpochSeconds = upload.photo.dateAddedEpochSeconds
        }

        lastSuccessfulEpochSeconds?.let { prefs.setLastSyncEpochMillis(it * 1000) }

        // More than this batch was pending, or something in it failed - either way there's still work
        // to do, so retry promptly (see the scheduler's short backoff criteria) instead of waiting for
        // the next full periodic interval.
        val moreWorkRemains = pending.size > batch.size
        return if (allSucceeded && !moreWorkRemains) Result.success() else Result.retry()
    }

    private suspend fun uploadOne(scanner: MediaStoreScanner, client: VaultApiClient, upload: PendingUpload): Boolean {
        val tempFile = File(applicationContext.cacheDir, "vault_upload_${upload.photo.id}")
        return try {
            val input = scanner.openInputStream(upload.photo) ?: return true // deleted since query - skip, not a failure
            input.use { stream -> tempFile.outputStream().use { output -> stream.copyTo(output) } }
            client.uploadFile(tempFile, upload.remotePath, upload.photo.mimeType)
        } finally {
            tempFile.delete()
        }
    }
}

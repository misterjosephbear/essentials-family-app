package com.isaacshub.app.vault.ui.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.isaacshub.app.App
import com.isaacshub.app.vault.backup.AppDataBackupScheduler
import com.isaacshub.app.vault.domain.VaultConnection
import com.isaacshub.app.vault.work.KEY_DISPLAY_NAME
import com.isaacshub.app.vault.work.KEY_MIME_TYPE
import com.isaacshub.app.vault.work.KEY_TARGET_PATH
import com.isaacshub.app.vault.work.KEY_URI
import com.isaacshub.app.vault.work.MANUAL_UPLOAD_TAG
import com.isaacshub.app.vault.work.ManualUploadWorker
import com.isaacshub.app.vault.work.PROGRESS_DISPLAY_NAME
import com.isaacshub.app.vault.work.PROGRESS_REMAINING
import com.isaacshub.app.vault.work.PhotoBackupScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultHomeUiState(
    val connection: VaultConnection? = null,
    val lastSyncEpochMillis: Long = 0,
    val lastBackupEpochMillis: Long = 0,
    val syncingPhotos: Boolean = false,
    val photosRemaining: Int? = null,
    val backingUpAppData: Boolean = false,
    val uploadingFileName: String? = null,
    val uploadError: String? = null
)

class VaultHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<App>().vaultPreferencesRepository
    private val workManager = WorkManager.getInstance(application)

    // Only the one-time ("now") work is meaningful "in progress" feedback for a button tap - the
    // periodic work sits in ENQUEUED (a non-"finished" state) essentially all the time between its
    // scheduled runs, so including it here would show "in progress" permanently.
    private val photoWorkInfos = workManager.getWorkInfosForUniqueWorkFlow(PhotoBackupScheduler.ONE_TIME_WORK_NAME)
    private val appDataWorkInfos = workManager.getWorkInfosForUniqueWorkFlow(AppDataBackupScheduler.ONE_TIME_WORK_NAME)

    // Backed by WorkManager (tagged, not a unique work name, since multiple manual uploads can be
    // queued at once) rather than in-memory ViewModel state - that's what lets an upload survive the
    // app being closed or the process dying mid-transfer instead of just vanishing with no trace.
    private val manualUploadWorkInfos = workManager.getWorkInfosByTagFlow(MANUAL_UPLOAD_TAG)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<VaultHomeUiState> = combine(
        prefs.connection,
        prefs.lastSyncEpochMillis,
        prefs.lastBackupEpochMillis,
        photoWorkInfos,
        appDataWorkInfos,
        manualUploadWorkInfos
    ) { flows ->
        val connection = flows[0] as VaultConnection?
        val lastSync = flows[1] as Long
        val lastBackup = flows[2] as Long
        val photoInfos = flows[3] as List<WorkInfo>
        val appDataInfos = flows[4] as List<WorkInfo>
        val manualUploadInfos = flows[5] as List<WorkInfo>

        val runningPhotoWork = photoInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        val activeUpload = manualUploadInfos.firstOrNull { !it.state.isFinished }
        val failedUpload = manualUploadInfos.lastOrNull { it.state == WorkInfo.State.FAILED }

        VaultHomeUiState(
            connection = connection,
            lastSyncEpochMillis = lastSync,
            lastBackupEpochMillis = lastBackup,
            syncingPhotos = photoInfos.any { !it.state.isFinished },
            photosRemaining = runningPhotoWork?.progress?.getInt(PROGRESS_REMAINING, -1)?.takeIf { it >= 0 },
            backingUpAppData = appDataInfos.any { !it.state.isFinished },
            uploadingFileName = activeUpload?.let { it.progress.getString(PROGRESS_DISPLAY_NAME) ?: "file" },
            uploadError = failedUpload?.outputData?.getString(PROGRESS_DISPLAY_NAME)?.let { "Couldn't upload $it" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultHomeUiState())

    fun syncNow() {
        PhotoBackupScheduler.syncNow(getApplication())
    }

    fun backupNow() {
        AppDataBackupScheduler.backupNow(getApplication())
    }

    /**
     * Uploads an arbitrary file the user picked (any type) to a "FromPhone/" folder in the vault -
     * for anything outside the automatic photo/video backup. Enqueued as WorkManager work (see
     * [ManualUploadWorker]) instead of run inline, so it survives the app closing or the process
     * dying mid-upload and resumes automatically once a network is available again.
     */
    fun uploadPickedFile(uri: Uri) {
        if (uiState.value.connection == null) return
        viewModelScope.launch {
            val context = getApplication<App>()
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "file"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val request = OneTimeWorkRequestBuilder<ManualUploadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .addTag(MANUAL_UPLOAD_TAG)
                .setInputData(
                    workDataOf(
                        KEY_URI to uri.toString(),
                        KEY_DISPLAY_NAME to displayName,
                        KEY_MIME_TYPE to mimeType,
                        KEY_TARGET_PATH to "FromPhone/$displayName"
                    )
                )
                .build()
            workManager.enqueue(request)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val context = getApplication<App>()
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    /**
     * Updates just the remote/tunnel URL for the current pairing (pass blank/null to clear it) -
     * lets it be set or changed (e.g. after setting up a playit.plus tunnel, or if it changes)
     * without unpairing and re-scanning a new QR code.
     */
    fun setRemoteBaseUrl(remoteBaseUrl: String) {
        viewModelScope.launch {
            prefs.setRemoteBaseUrl(remoteBaseUrl.trim().trimEnd('/').ifBlank { null })
        }
    }

    fun unpair() {
        viewModelScope.launch {
            PhotoBackupScheduler.cancel(getApplication())
            AppDataBackupScheduler.cancel(getApplication())
            workManager.cancelAllWorkByTag(MANUAL_UPLOAD_TAG)
            prefs.clearConnection()
        }
    }
}

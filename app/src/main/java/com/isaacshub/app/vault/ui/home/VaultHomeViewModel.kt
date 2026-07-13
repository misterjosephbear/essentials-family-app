package com.isaacshub.app.vault.ui.home

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.isaacshub.app.App
import com.isaacshub.app.vault.backup.AppDataBackupScheduler
import com.isaacshub.app.vault.data.VaultApiClient
import com.isaacshub.app.vault.domain.VaultConnection
import com.isaacshub.app.vault.work.PROGRESS_REMAINING
import com.isaacshub.app.vault.work.PhotoBackupScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val uploadState = MutableStateFlow(Pair<String?, String?>(null, null))

    private val backupStatus = combine(
        prefs.connection,
        prefs.lastSyncEpochMillis,
        prefs.lastBackupEpochMillis,
        photoWorkInfos,
        appDataWorkInfos
    ) { connection, lastSync, lastBackup, photoInfos, appDataInfos ->
        val runningPhotoWork = photoInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        VaultHomeUiState(
            connection = connection,
            lastSyncEpochMillis = lastSync,
            lastBackupEpochMillis = lastBackup,
            syncingPhotos = photoInfos.any { !it.state.isFinished },
            photosRemaining = runningPhotoWork?.progress?.getInt(PROGRESS_REMAINING, -1)?.takeIf { it >= 0 },
            backingUpAppData = appDataInfos.any { !it.state.isFinished }
        )
    }

    val uiState: StateFlow<VaultHomeUiState> = combine(backupStatus, uploadState) { status, upload ->
        status.copy(uploadingFileName = upload.first, uploadError = upload.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultHomeUiState())

    fun syncNow() {
        PhotoBackupScheduler.syncNow(getApplication())
    }

    fun backupNow() {
        AppDataBackupScheduler.backupNow(getApplication())
    }

    /** Uploads an arbitrary file the user picked (any type) to a "FromPhone/" folder in the vault - for anything outside the automatic photo/video backup. */
    fun uploadPickedFile(uri: Uri) {
        val connection = uiState.value.connection ?: return
        viewModelScope.launch {
            val context = getApplication<App>()
            val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "file"
            uploadState.value = displayName to null

            val tempFile = File(context.cacheDir, "vault_manual_upload_${System.currentTimeMillis()}")
            val uploaded = withContext(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(uri) ?: return@withContext false
                    input.use { stream -> tempFile.outputStream().use { output -> stream.copyTo(output) } }
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    VaultApiClient(connection).uploadFile(tempFile, "FromPhone/$displayName", mimeType)
                } catch (_: Exception) {
                    false
                } finally {
                    tempFile.delete()
                }
            }

            uploadState.value = if (uploaded) {
                null to null
            } else {
                null to "Couldn't upload $displayName"
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val context = getApplication<App>()
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    fun unpair() {
        viewModelScope.launch {
            PhotoBackupScheduler.cancel(getApplication())
            AppDataBackupScheduler.cancel(getApplication())
            prefs.clearConnection()
        }
    }
}

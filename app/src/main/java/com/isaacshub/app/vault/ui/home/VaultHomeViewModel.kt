package com.isaacshub.app.vault.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.isaacshub.app.App
import com.isaacshub.app.vault.backup.AppDataBackupScheduler
import com.isaacshub.app.vault.domain.VaultConnection
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
    val backingUpAppData: Boolean = false
)

class VaultHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<App>().vaultPreferencesRepository
    private val workManager = WorkManager.getInstance(application)

    // Only the one-time ("now") work is meaningful "in progress" feedback for a button tap - the
    // periodic work sits in ENQUEUED (a non-"finished" state) essentially all the time between its
    // scheduled runs, so including it here would show "in progress" permanently.
    private val photoWorkInfos = workManager.getWorkInfosForUniqueWorkFlow(PhotoBackupScheduler.ONE_TIME_WORK_NAME)
    private val appDataWorkInfos = workManager.getWorkInfosForUniqueWorkFlow(AppDataBackupScheduler.ONE_TIME_WORK_NAME)

    val uiState: StateFlow<VaultHomeUiState> = combine(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultHomeUiState())

    fun syncNow() {
        PhotoBackupScheduler.syncNow(getApplication())
    }

    fun backupNow() {
        AppDataBackupScheduler.backupNow(getApplication())
    }

    fun unpair() {
        viewModelScope.launch {
            PhotoBackupScheduler.cancel(getApplication())
            AppDataBackupScheduler.cancel(getApplication())
            prefs.clearConnection()
        }
    }
}

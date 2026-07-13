package com.isaacshub.app.vault.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.vault.domain.VaultConnection
import com.isaacshub.app.vault.work.PhotoBackupScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultHomeUiState(
    val connection: VaultConnection? = null,
    val lastSyncEpochMillis: Long = 0
)

class VaultHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<App>().vaultPreferencesRepository

    val uiState: StateFlow<VaultHomeUiState> = combine(
        prefs.connection,
        prefs.lastSyncEpochMillis
    ) { connection, lastSync -> VaultHomeUiState(connection, lastSync) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultHomeUiState())

    fun syncNow() {
        PhotoBackupScheduler.syncNow(getApplication())
    }

    fun unpair() {
        viewModelScope.launch {
            PhotoBackupScheduler.cancel(getApplication())
            prefs.clearConnection()
        }
    }
}

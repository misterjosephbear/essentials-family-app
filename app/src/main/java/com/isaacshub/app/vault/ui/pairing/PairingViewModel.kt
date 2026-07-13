package com.isaacshub.app.vault.ui.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.vault.backup.AppDataBackupScheduler
import com.isaacshub.app.vault.data.VaultApiClient
import com.isaacshub.app.vault.domain.parsePairingPayload
import com.isaacshub.app.vault.work.PhotoBackupScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairingUiState(
    val connecting: Boolean = false,
    val error: String? = null,
    val paired: Boolean = false
)

class PairingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var handling = false

    /** Called for every decoded QR payload from the camera - ignores anything that isn't a valid pairing code. */
    fun onQrCodeScanned(raw: String) {
        if (handling) return
        val connection = parsePairingPayload(raw)
        if (connection == null) {
            _uiState.value = _uiState.value.copy(error = "That doesn't look like an Isaac's Hub Storage pairing code")
            return
        }
        handling = true
        _uiState.value = _uiState.value.copy(connecting = true, error = null)
        viewModelScope.launch {
            val ok = VaultApiClient(connection).checkConnection()
            if (ok) {
                val app = getApplication<App>()
                app.vaultPreferencesRepository.setConnection(connection)
                PhotoBackupScheduler.schedule(app)
                AppDataBackupScheduler.schedule(app)
                AppDataBackupScheduler.backupNow(app)
                _uiState.value = _uiState.value.copy(connecting = false, paired = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    connecting = false,
                    error = "Couldn't connect - check that the server is reachable and awake"
                )
                handling = false
            }
        }
    }
}

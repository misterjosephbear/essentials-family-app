package com.isaacshub.app.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.settings.data.DebugTriggerResult
import com.isaacshub.app.settings.data.SettingsApiClient
import com.isaacshub.app.vault.data.VaultPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface DebugBridgeUiState {
    data object Idle : DebugBridgeUiState
    data object Triggering : DebugBridgeUiState
    data object Started : DebugBridgeUiState
    data class Error(val message: String) : DebugBridgeUiState
}

class SettingsViewModel(
    private val vaultPreferencesRepository: VaultPreferencesRepository
) : ViewModel() {

    private val _debugBridgeState = MutableStateFlow<DebugBridgeUiState>(DebugBridgeUiState.Idle)
    val debugBridgeState: StateFlow<DebugBridgeUiState> = _debugBridgeState

    fun debugDiscordBridge() {
        if (_debugBridgeState.value == DebugBridgeUiState.Triggering) return
        viewModelScope.launch {
            _debugBridgeState.value = DebugBridgeUiState.Triggering
            val connection = vaultPreferencesRepository.connection.first()
            if (connection == null) {
                _debugBridgeState.value = DebugBridgeUiState.Error("Pair with a server first (Photo Vault tab).")
                return@launch
            }
            _debugBridgeState.value = when (val result = SettingsApiClient(connection).triggerDiscordBridgeDebug()) {
                is DebugTriggerResult.Started -> DebugBridgeUiState.Started
                is DebugTriggerResult.AlreadyRunning -> DebugBridgeUiState.Error("A debug session is already running - check Discord.")
                is DebugTriggerResult.Failed -> DebugBridgeUiState.Error(result.message)
            }
        }
    }

    class Factory(private val vaultPreferencesRepository: VaultPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(vaultPreferencesRepository) as T
    }
}

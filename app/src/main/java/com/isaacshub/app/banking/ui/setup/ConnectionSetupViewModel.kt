package com.isaacshub.app.banking.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.banking.data.BankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ConnectionSetupUiState {
    data object Idle : ConnectionSetupUiState
    data object Loading : ConnectionSetupUiState
    data object Success : ConnectionSetupUiState
    data class Error(val message: String) : ConnectionSetupUiState
}

class ConnectionSetupViewModel(
    private val repository: BankingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectionSetupUiState>(ConnectionSetupUiState.Idle)
    val uiState: StateFlow<ConnectionSetupUiState> = _uiState

    fun addSimpleFINConnection(setupToken: String) {
        if (setupToken.isBlank()) {
            _uiState.value = ConnectionSetupUiState.Error("Setup token cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = ConnectionSetupUiState.Loading

            repository.addSimpleFINConnection(setupToken)
                .onSuccess { connectionId ->
                    // Now sync the accounts
                    repository.syncAccounts(connectionId)
                        .onSuccess {
                            _uiState.value = ConnectionSetupUiState.Success
                        }
                        .onFailure { error ->
                            _uiState.value = ConnectionSetupUiState.Error(
                                "Connection added but failed to sync accounts: ${error.message}"
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = ConnectionSetupUiState.Error(
                        error.message ?: "Failed to add connection"
                    )
                }
        }
    }

    class Factory(private val repository: BankingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConnectionSetupViewModel(repository) as T
    }
}

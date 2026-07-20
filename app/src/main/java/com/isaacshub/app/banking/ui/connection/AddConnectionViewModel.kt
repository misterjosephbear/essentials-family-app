package com.isaacshub.app.banking.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.banking.data.BankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AddConnectionUiState {
    data object Idle : AddConnectionUiState
    data object Processing : AddConnectionUiState
    data object Success : AddConnectionUiState
    data class Error(val message: String) : AddConnectionUiState
}

class AddConnectionViewModel(
    private val repository: BankingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddConnectionUiState>(AddConnectionUiState.Idle)
    val uiState: StateFlow<AddConnectionUiState> = _uiState

    fun addSimpleFINConnection(setupToken: String) {
        if (setupToken.isBlank()) {
            _uiState.value = AddConnectionUiState.Error("Setup token cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddConnectionUiState.Processing

            repository.addSimpleFINConnection(setupToken)
                .onSuccess { connectionId ->
                    // Immediately sync accounts for the new connection
                    repository.syncAccounts(connectionId)
                        .onSuccess {
                            _uiState.value = AddConnectionUiState.Success
                        }
                        .onFailure { error ->
                            _uiState.value = AddConnectionUiState.Error(
                                "Connection added but failed to sync accounts: ${error.message}"
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = AddConnectionUiState.Error(
                        "Failed to add connection: ${error.message}"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = AddConnectionUiState.Idle
    }

    class Factory(private val repository: BankingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddConnectionViewModel(repository) as T
    }
}

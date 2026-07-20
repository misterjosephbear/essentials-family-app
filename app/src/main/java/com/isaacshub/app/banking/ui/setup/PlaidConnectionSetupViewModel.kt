package com.isaacshub.app.banking.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.banking.data.BankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface PlaidConnectionSetupUiState {
    data object Idle : PlaidConnectionSetupUiState
    data object CreatingLinkToken : PlaidConnectionSetupUiState
    data class ReadyToLaunchLink(val linkToken: String) : PlaidConnectionSetupUiState
    data object ExchangingToken : PlaidConnectionSetupUiState
    data object Success : PlaidConnectionSetupUiState
    data class Error(val message: String) : PlaidConnectionSetupUiState
}

class PlaidConnectionSetupViewModel(
    private val repository: BankingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaidConnectionSetupUiState>(PlaidConnectionSetupUiState.Idle)
    val uiState: StateFlow<PlaidConnectionSetupUiState> = _uiState

    fun startPlaidLink() {
        viewModelScope.launch {
            _uiState.value = PlaidConnectionSetupUiState.CreatingLinkToken

            // Create a unique user ID for this connection
            val userId = UUID.randomUUID().toString()

            repository.createLinkToken(userId)
                .onSuccess { linkToken ->
                    _uiState.value = PlaidConnectionSetupUiState.ReadyToLaunchLink(linkToken)
                }
                .onFailure { error ->
                    _uiState.value = PlaidConnectionSetupUiState.Error(
                        error.message ?: "Failed to initialize Plaid Link"
                    )
                }
        }
    }

    fun handlePlaidSuccess(publicToken: String) {
        viewModelScope.launch {
            _uiState.value = PlaidConnectionSetupUiState.ExchangingToken

            repository.addPlaidConnection(publicToken)
                .onSuccess {
                    _uiState.value = PlaidConnectionSetupUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = PlaidConnectionSetupUiState.Error(
                        error.message ?: "Failed to add bank connection"
                    )
                }
        }
    }

    fun handlePlaidExit() {
        // User canceled or exited Plaid Link
        _uiState.value = PlaidConnectionSetupUiState.Idle
    }

    class Factory(private val repository: BankingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaidConnectionSetupViewModel(repository) as T
    }
}

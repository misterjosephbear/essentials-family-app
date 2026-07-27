package com.isaacshub.app.banking.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.banking.data.BankingRepository
import com.isaacshub.app.banking.domain.BankAccount
import com.isaacshub.app.banking.domain.BankConnection
import com.isaacshub.app.banking.domain.BudgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BankingUiState {
    data object Loading : BankingUiState
    data class Success(
        val accounts: List<BankAccount>,
        val connections: List<BankConnection>,
        val totalBalance: Double,
        val budgetState: BudgetState?,
        val isRefreshing: Boolean
    ) : BankingUiState
    data class Error(val message: String) : BankingUiState
}

class BankingHomeViewModel(
    private val repository: BankingRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BankingUiState> = combine(
        repository.observeAllAccounts(),
        repository.observeAllConnections(),
        repository.observeBudgetState(),
        _isRefreshing,
        _errorMessage
    ) { accounts, connections, budgetState, isRefreshing, error ->
        when {
            error != null -> BankingUiState.Error(error)
            else -> BankingUiState.Success(
                accounts = accounts,
                connections = connections,
                totalBalance = accounts.sumOf { it.balance },
                budgetState = budgetState,
                isRefreshing = isRefreshing
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BankingUiState.Loading
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val state = uiState.value
            if (state is BankingUiState.Success) {
                state.connections.forEach { connection ->
                    repository.syncAccounts(connection.id).onFailure { error ->
                        _errorMessage.value = "Failed to sync ${connection.institutionName}: ${error.message}"
                    }
                }
            }

            _isRefreshing.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(private val repository: BankingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BankingHomeViewModel(repository) as T
    }
}

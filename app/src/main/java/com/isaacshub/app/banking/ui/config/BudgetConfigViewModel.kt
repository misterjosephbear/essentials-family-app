package com.isaacshub.app.banking.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.banking.data.BankingRepository
import com.isaacshub.app.banking.domain.BankAccount
import com.isaacshub.app.banking.domain.BudgetCategory
import com.isaacshub.app.banking.domain.BudgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetConfigUiState(
    val categories: List<BudgetCategory> = emptyList(),
    val accounts: List<BankAccount> = emptyList(),
    val selectedAccountIds: Set<String> = emptySet(),
    val budgetState: BudgetState? = null,
    val showAccountPicker: Boolean = false,
    val editingCategory: BudgetCategory? = null,
    val isLoading: Boolean = false
)

class BudgetConfigViewModel(
    private val repository: BankingRepository
) : ViewModel() {

    private val _showAccountPicker = MutableStateFlow(false)
    private val _editingCategory = MutableStateFlow<BudgetCategory?>(null)

    val uiState: StateFlow<BudgetConfigUiState> = combine(
        combine(
            repository.observeBudgetCategories(),
            repository.observeAllAccounts(),
            repository.observeSelectedAccounts(),
            repository.observeBudgetState()
        ) { categories, allAccounts, selectedAccounts, budgetState ->
            BudgetConfigUiState(
                categories = categories.sortedBy { it.order },
                accounts = allAccounts,
                selectedAccountIds = selectedAccounts.map { it.id }.toSet(),
                budgetState = budgetState,
                isLoading = false
            )
        },
        _showAccountPicker,
        _editingCategory
    ) { state, showPicker, editingCat ->
        state.copy(
            showAccountPicker = showPicker,
            editingCategory = editingCat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetConfigUiState(isLoading = true)
    )

    fun toggleAccountSelection(accountId: String) {
        viewModelScope.launch {
            val currentlyIncluded = repository.isAccountIncluded(accountId)
            repository.setAccountIncluded(accountId, !currentlyIncluded)
        }
    }

    fun saveCategoryThreshold(categoryId: String, newThreshold: Double) {
        viewModelScope.launch {
            val category = uiState.value.categories.find { it.id == categoryId }
            category?.let {
                repository.saveBudgetCategory(it.copy(threshold = newThreshold))
            }
            closeEditDialog()
        }
    }

    fun openAccountPicker() {
        _showAccountPicker.value = true
    }

    fun closeAccountPicker() {
        _showAccountPicker.value = false
    }

    fun editCategory(category: BudgetCategory) {
        _editingCategory.value = category
    }

    fun closeEditDialog() {
        _editingCategory.value = null
    }

    class Factory(
        private val repository: BankingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetConfigViewModel::class.java)) {
                return BudgetConfigViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

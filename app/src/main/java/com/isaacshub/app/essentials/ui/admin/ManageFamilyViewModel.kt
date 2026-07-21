package com.isaacshub.app.essentials.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.essentials.data.ChildAccountEntity
import com.isaacshub.app.essentials.data.EssentialsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageFamilyUiState(
    val error: String? = null,
    val isLoading: Boolean = false
)

class ManageFamilyViewModel(
    private val essentialsRepository: EssentialsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageFamilyUiState())
    val uiState: StateFlow<ManageFamilyUiState> = _uiState.asStateFlow()

    val children: StateFlow<List<ChildAccountEntity>> = essentialsRepository.observeAllChildren()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createChild(username: String, displayName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                essentialsRepository.createChildAccount(username, displayName)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create child account",
                    isLoading = false
                )
            }
        }
    }

    fun updateChild(id: Long, username: String, displayName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                essentialsRepository.updateChildAccount(id, username, displayName)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update child account",
                    isLoading = false
                )
            }
        }
    }

    fun deleteChild(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                essentialsRepository.deleteChildAccount(id)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete child account",
                    isLoading = false
                )
            }
        }
    }

    class Factory(
        private val essentialsRepository: EssentialsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ManageFamilyViewModel(essentialsRepository) as T
    }
}

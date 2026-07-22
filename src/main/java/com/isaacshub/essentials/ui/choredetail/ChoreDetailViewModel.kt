package com.isaacshub.essentials.ui.choredetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.essentials.data.local.entities.CompletionStatus
import com.isaacshub.essentials.data.local.entities.LocalChoreEntity
import com.isaacshub.essentials.data.local.entities.LocalCompletionEntity
import com.isaacshub.essentials.data.repository.EssentialsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChoreDetailUiState(
    val chore: LocalChoreEntity? = null,
    val completion: LocalCompletionEntity? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val canComplete: Boolean
        get() = chore != null && completion == null

    val canRetake: Boolean
        get() = completion?.status == CompletionStatus.REJECTED

    val isCompleted: Boolean
        get() = completion != null

    val isVerified: Boolean
        get() = completion?.status == CompletionStatus.VERIFIED

    val isPending: Boolean
        get() = completion?.status == CompletionStatus.PENDING_VERIFICATION

    val isRejected: Boolean
        get() = completion?.status == CompletionStatus.REJECTED

    val needsPhoto: Boolean
        get() = chore?.photoRequirement != null
}

class ChoreDetailViewModel(
    private val choreId: Long,
    private val essentialsRepository: EssentialsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoreDetailUiState())
    val uiState: StateFlow<ChoreDetailUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()

    init {
        loadChoreDetail()
    }

    private fun loadChoreDetail() {
        viewModelScope.launch {
            combine(
                essentialsRepository.observeChoreById(choreId),
                essentialsRepository.observeCompletionByChoreAndDate(choreId, today)
            ) { chore, completion ->
                ChoreDetailUiState(
                    chore = chore,
                    completion = completion,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun completeWithoutPhoto(onCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                essentialsRepository.markChoreCompleted(
                    choreId = choreId,
                    date = today,
                    photoUri = null
                )
                onCompleted()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to complete chore: ${e.message}")
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val choreId: Long,
        private val essentialsRepository: EssentialsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChoreDetailViewModel(choreId, essentialsRepository) as T
        }
    }
}

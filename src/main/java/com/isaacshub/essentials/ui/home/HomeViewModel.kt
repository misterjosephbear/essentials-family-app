package com.isaacshub.essentials.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.essentials.data.local.entities.LocalChoreEntity
import com.isaacshub.essentials.data.local.entities.LocalCompletionEntity
import com.isaacshub.essentials.data.repository.AuthRepository
import com.isaacshub.essentials.data.repository.EssentialsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class ChoreUiState(
    val chore: LocalChoreEntity,
    val completion: LocalCompletionEntity?
) {
    val isCompleted: Boolean
        get() = completion != null

    val isVerified: Boolean
        get() = completion?.status == com.isaacshub.essentials.data.local.entities.CompletionStatus.VERIFIED

    val isPending: Boolean
        get() = completion?.status == com.isaacshub.essentials.data.local.entities.CompletionStatus.PENDING_VERIFICATION

    val isRejected: Boolean
        get() = completion?.status == com.isaacshub.essentials.data.local.entities.CompletionStatus.REJECTED
}

data class HomeUiState(
    val displayName: String = "",
    val todaysChores: List<ChoreUiState> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val completionPercentage: Int = 0
)

class HomeViewModel(
    private val essentialsRepository: EssentialsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()
    private val todayDayOfWeek = today.dayOfWeek

    init {
        loadUserData()
        observeChoresAndCompletions()
        syncChores()
    }

    private fun loadUserData() {
        val authToken = authRepository.getAuthToken()
        if (authToken != null) {
            _uiState.update { it.copy(displayName = authToken.displayName) }
        }
    }

    private fun observeChoresAndCompletions() {
        viewModelScope.launch {
            combine(
                essentialsRepository.observeAllChores(),
                essentialsRepository.observeCompletionsByDate(today)
            ) { chores, completions ->
                // Filter chores for today
                val todaysChores = chores.filter { chore ->
                    todayDayOfWeek in chore.daysOfWeek
                }

                // Map to UI state with completion info
                val choreUiStates = todaysChores.map { chore ->
                    val completion = completions.find { it.choreId == chore.id }
                    ChoreUiState(chore, completion)
                }

                // Calculate completion percentage
                val completedCount = choreUiStates.count { it.isCompleted }
                val percentage = if (choreUiStates.isNotEmpty()) {
                    (completedCount * 100) / choreUiStates.size
                } else {
                    0
                }

                choreUiStates to percentage
            }.collect { (choreUiStates, percentage) ->
                _uiState.update {
                    it.copy(
                        todaysChores = choreUiStates,
                        completionPercentage = percentage,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun syncChores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }

            essentialsRepository.syncChoresFromServer()
                .onSuccess {
                    _uiState.update { it.copy(isSyncing = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = "Sync failed: ${error.message}"
                        )
                    }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.clearAuthToken()
            onLogoutComplete()
        }
    }

    class Factory(
        private val essentialsRepository: EssentialsRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(essentialsRepository, authRepository) as T
        }
    }
}

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
import java.time.DayOfWeek

data class CreateChoreUiState(
    val choreId: Long? = null,
    val name: String = "",
    val description: String = "",
    val photoRequirement: String? = null,
    val requirePhoto: Boolean = false,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val assignedChildIds: List<Long> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val isLoading: Boolean = false
)

class CreateChoreViewModel(
    private val essentialsRepository: EssentialsRepository,
    choreId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateChoreUiState(choreId = choreId))
    val uiState: StateFlow<CreateChoreUiState> = _uiState.asStateFlow()

    val children: StateFlow<List<ChildAccountEntity>> = essentialsRepository.observeAllChildren()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (choreId != null) {
            loadChore(choreId)
        }
    }

    private fun loadChore(choreId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val chore = essentialsRepository.getChoreById(choreId)
            if (chore != null) {
                _uiState.value = _uiState.value.copy(
                    name = chore.name,
                    description = chore.description,
                    photoRequirement = chore.photoRequirement,
                    requirePhoto = chore.photoRequirement != null,
                    selectedDays = chore.daysOfWeek.toSet(),
                    assignedChildIds = chore.assignedChildIds,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Chore not found",
                    isLoading = false
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description, error = null)
    }

    fun setPhotoRequirement(requirement: String) {
        _uiState.value = _uiState.value.copy(photoRequirement = requirement, error = null)
    }

    fun setRequirePhoto(require: Boolean) {
        _uiState.value = _uiState.value.copy(
            requirePhoto = require,
            photoRequirement = if (!require) null else _uiState.value.photoRequirement,
            error = null
        )
    }

    fun toggleDay(day: DayOfWeek) {
        val current = _uiState.value.selectedDays
        val updated = if (current.contains(day)) {
            current - day
        } else {
            current + day
        }
        _uiState.value = _uiState.value.copy(selectedDays = updated, error = null)
    }

    fun toggleChild(childId: Long) {
        val current = _uiState.value.assignedChildIds
        val updated = if (current.contains(childId)) {
            current - childId
        } else {
            current + childId
        }
        _uiState.value = _uiState.value.copy(assignedChildIds = updated, error = null)
    }

    fun save() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Chore name cannot be blank")
            return
        }

        if (state.selectedDays.isEmpty()) {
            _uiState.value = state.copy(error = "Must select at least one day")
            return
        }

        if (state.requirePhoto && state.photoRequirement.isNullOrBlank()) {
            _uiState.value = state.copy(error = "Photo requirement description cannot be blank when photo is required")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true)

                val choreId = state.choreId
                if (choreId == null) {
                    // Create new chore
                    essentialsRepository.createChore(
                        name = state.name,
                        description = state.description,
                        photoRequirement = if (state.requirePhoto) state.photoRequirement else null,
                        daysOfWeek = state.selectedDays.toList(),
                        assignedChildIds = state.assignedChildIds
                    )
                } else {
                    // Update existing chore
                    essentialsRepository.updateChore(
                        id = choreId,
                        name = state.name,
                        description = state.description,
                        photoRequirement = if (state.requirePhoto) state.photoRequirement else null,
                        daysOfWeek = state.selectedDays.toList(),
                        assignedChildIds = state.assignedChildIds
                    )
                }

                _uiState.value = _uiState.value.copy(saved = true, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save chore",
                    isLoading = false
                )
            }
        }
    }

    class Factory(
        private val essentialsRepository: EssentialsRepository,
        private val choreId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreateChoreViewModel(essentialsRepository, choreId) as T
    }
}

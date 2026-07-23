package com.isaacshub.app.featurefunnel.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.featurefunnel.data.FeatureFunnelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditPromptUiState(
    val title: String = "",
    val promptText: String = "",
    val priority: Int = 0,
    val canSave: Boolean = false
)

class EditPromptViewModel(
    private val repository: FeatureFunnelRepository,
    private val promptId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditPromptUiState())
    val uiState: StateFlow<EditPromptUiState> = _uiState.asStateFlow()

    init {
        if (promptId != null) {
            loadPrompt(promptId)
        }
    }

    private fun loadPrompt(id: Long) {
        viewModelScope.launch {
            val prompt = repository.getById(id)
            if (prompt != null) {
                _uiState.value = EditPromptUiState(
                    title = prompt.title,
                    promptText = prompt.promptText,
                    priority = prompt.priority,
                    canSave = true
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(
            title = title,
            canSave = title.isNotBlank() && _uiState.value.promptText.isNotBlank()
        )
    }

    fun updatePromptText(text: String) {
        _uiState.value = _uiState.value.copy(
            promptText = text,
            canSave = _uiState.value.title.isNotBlank() && text.isNotBlank()
        )
    }

    fun updatePriority(priority: Int) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            if (promptId == null) {
                repository.createPrompt(
                    title = state.title,
                    promptText = state.promptText,
                    priority = state.priority
                )
            } else {
                val existing = repository.getById(promptId)
                if (existing != null) {
                    repository.updatePrompt(
                        existing.copy(
                            title = state.title,
                            promptText = state.promptText,
                            priority = state.priority
                        )
                    )
                }
            }
        }
    }

    class Factory(
        private val repository: FeatureFunnelRepository,
        private val promptId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditPromptViewModel(repository, promptId) as T
    }
}

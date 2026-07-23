package com.isaacshub.app.featurefunnel.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.featurefunnel.data.*
import com.isaacshub.app.featurefunnel.worker.FeatureFunnelScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FeatureFunnelUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class FeatureFunnelHomeViewModel(
    private val repository: FeatureFunnelRepository,
    private val preferencesRepository: FeatureFunnelPreferencesRepository,
    private val context: Context
) : ViewModel() {

    val prompts: StateFlow<List<FeaturePromptEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<FeatureFunnelPreferences> = preferencesRepository.preferences
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FeatureFunnelPreferences()
        )

    private val _uiState = MutableStateFlow(FeatureFunnelUiState())
    val uiState: StateFlow<FeatureFunnelUiState> = _uiState.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnabled(enabled)
            if (enabled) {
                FeatureFunnelScheduler.schedule(context)
            } else {
                FeatureFunnelScheduler.cancel(context)
            }
        }
    }

    fun deletePrompt(prompt: FeaturePromptEntity) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
        }
    }

    fun retryPrompt(promptId: Long) {
        viewModelScope.launch {
            repository.retryPrompt(promptId)
        }
    }

    fun pausePrompt(promptId: Long) {
        viewModelScope.launch {
            repository.pausePrompt(promptId)
        }
    }

    fun resumePrompt(promptId: Long) {
        viewModelScope.launch {
            repository.resumePrompt(promptId)
        }
    }

    fun checkNow() {
        FeatureFunnelScheduler.checkNow(context)
    }

    class Factory(
        private val repository: FeatureFunnelRepository,
        private val preferencesRepository: FeatureFunnelPreferencesRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FeatureFunnelHomeViewModel(repository, preferencesRepository, context) as T
    }
}

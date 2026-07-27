package com.isaacshub.app.activitymapper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.activitymapper.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActivityMapperUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class ActivityMapperViewModel(
    private val repository: ActivityMapperRepository
) : ViewModel() {

    val rules: StateFlow<List<AutomationRule>> = repository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val richPresenceProfiles: StateFlow<List<DiscordRichPresenceProfile>> = repository.richPresenceProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val variables: StateFlow<List<VariableValue>> = repository.variables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = repository.isLoading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val error: StateFlow<String?> = repository.error
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun toggleRuleEnabled(rule: AutomationRule) {
        viewModelScope.launch {
            repository.toggleRuleEnabled(rule)
        }
    }

    fun deleteRule(ruleId: Int) {
        viewModelScope.launch {
            repository.deleteRule(ruleId)
        }
    }

    fun deleteRichPresenceProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteRichPresenceProfile(profileId)
        }
    }

    fun clearError() {
        repository.clearError()
    }

    class Factory(
        private val repository: ActivityMapperRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ActivityMapperViewModel(repository) as T
    }
}

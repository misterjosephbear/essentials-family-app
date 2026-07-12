package com.isaacshub.app.sleep.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.core.data.prefs.UserPreferences
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setSleepNeedMinutes(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setSleepNeedMinutes(minutes) }
    }

    fun setAutoDetectEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoDetectEnabled(enabled) }
    }

    class Factory(private val preferencesRepository: UserPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(preferencesRepository) as T
    }
}

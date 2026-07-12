package com.isaacshub.app.sleep.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.sleep.data.SleepRepository
import com.isaacshub.app.sleep.data.SleepSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val sleepRepository: SleepRepository) : ViewModel() {

    val sessions: StateFlow<List<SleepSessionEntity>> = sleepRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(session: SleepSessionEntity) {
        viewModelScope.launch { sleepRepository.deleteSession(session) }
    }

    class Factory(private val sleepRepository: SleepRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(sleepRepository) as T
    }
}

package com.isaacshub.app.timetracking.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.timetracking.domain.ScheduledDay
import com.isaacshub.app.timetracking.domain.computeWeekSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WeekScheduleViewModel(repository: TimeTrackingRepository) : ViewModel() {

    val days: StateFlow<List<ScheduledDay>> = combine(
        repository.observeEntries(),
        repository.observeRoutes()
    ) { entries, routes -> computeWeekSchedule(entries, routes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(private val repository: TimeTrackingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeekScheduleViewModel(repository) as T
    }
}

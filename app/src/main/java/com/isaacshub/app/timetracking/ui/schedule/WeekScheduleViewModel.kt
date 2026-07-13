package com.isaacshub.app.timetracking.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.RouteScheduleOverrideEntity
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.timetracking.domain.ScheduledDay
import com.isaacshub.app.timetracking.domain.WeeklySummary
import com.isaacshub.app.timetracking.domain.computeWeekSchedule
import com.isaacshub.app.timetracking.domain.computeWeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WeekScheduleUiState(
    val weekOffset: Int = 0,
    val summary: WeeklySummary = computeWeeklySummary(emptyList()),
    val days: List<ScheduledDay> = emptyList(),
    val routes: List<RouteEntity> = emptyList()
)

/**
 * [weekOffset] is in whole weeks from the current one (0 = this week, -1 = last week, +1 = next week).
 * An anchor date is derived by shifting today by that many weeks - since a week is exactly 7 days, this
 * lands on the same day-of-week and so resolves to the right Sat-Fri week for every downstream calculation.
 */
class WeekScheduleViewModel(private val repository: TimeTrackingRepository) : ViewModel() {

    private val weekOffset = MutableStateFlow(0)

    val uiState: StateFlow<WeekScheduleUiState> = combine(
        repository.observeEntries(),
        repository.observeRoutes(),
        repository.observeScheduleOverrides(),
        weekOffset
    ) { entries, routes, overrides, offset ->
        val anchor = LocalDate.now().plusWeeks(offset.toLong())
        WeekScheduleUiState(
            weekOffset = offset,
            summary = computeWeeklySummary(entries, routes, today = anchor, overrides = overrides),
            days = computeWeekSchedule(entries, routes, today = anchor, overrides = overrides),
            routes = routes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekScheduleUiState())

    fun previousWeek() {
        weekOffset.value -= 1
    }

    fun nextWeek() {
        weekOffset.value += 1
    }

    fun goToCurrentWeek() {
        weekOffset.value = 0
    }

    fun addScheduleOverrides(routeId: Long, dates: List<LocalDate>) {
        viewModelScope.launch {
            dates.forEach { date ->
                repository.addScheduleOverride(RouteScheduleOverrideEntity(routeId = routeId, epochDay = date.toEpochDay()))
            }
        }
    }

    fun removeScheduleOverride(overrideId: Long) {
        viewModelScope.launch { repository.deleteScheduleOverride(overrideId) }
    }

    class Factory(private val repository: TimeTrackingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeekScheduleViewModel(repository) as T
    }
}

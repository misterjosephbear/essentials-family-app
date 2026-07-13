package com.isaacshub.app.timetracking.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.timetracking.domain.PayPeriodSummary
import com.isaacshub.app.timetracking.domain.WeeklySummary
import com.isaacshub.app.timetracking.domain.computePayPeriodSummary
import com.isaacshub.app.timetracking.domain.computeWeeklySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimeTrackingHomeUiState(
    val loading: Boolean = true,
    val summary: WeeklySummary = computeWeeklySummary(emptyList()),
    val payPeriod: PayPeriodSummary = computePayPeriodSummary(emptyList(), emptyList(), 0.0, 1.5, 0.0)
)

class TimeTrackingHomeViewModel(
    private val repository: TimeTrackingRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<TimeTrackingHomeUiState> = combine(
        repository.observeEntries(),
        repository.observeRoutes(),
        repository.observeDeductions(),
        repository.observeScheduleOverrides(),
        preferencesRepository.userPreferences
    ) { entries, routes, deductions, overrides, prefs ->
        TimeTrackingHomeUiState(
            loading = false,
            summary = computeWeeklySummary(entries, routes, overrides = overrides),
            payPeriod = computePayPeriodSummary(
                entries = entries,
                routes = routes,
                hourlyRate = prefs.hourlyRate,
                overtimeMultiplier = prefs.overtimeMultiplier,
                emaRate = prefs.emaRate,
                deductions = deductions,
                overrides = overrides
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeTrackingHomeUiState())

    fun deleteEntry(entry: TimeEntryEntity) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    class Factory(
        private val repository: TimeTrackingRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TimeTrackingHomeViewModel(repository, preferencesRepository) as T
    }
}

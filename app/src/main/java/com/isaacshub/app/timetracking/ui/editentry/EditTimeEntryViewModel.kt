package com.isaacshub.app.timetracking.ui.editentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class EditTimeEntryUiState(
    val entryId: Long? = null,
    val createdAtEpochMillis: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val payType: PayType = PayType.HOURLY,
    val routeId: Long? = null,
    val label: String = "",
    val startTime: LocalTime = LocalTime.of(8, 0),
    val endTime: LocalTime = LocalTime.of(16, 0),
    val notes: String = "",
    val routes: List<RouteEntity> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false
) {
    /** Actual clocked hours for this shift, wrapping past midnight if the end time is before the start time. */
    val actualHours: Double
        get() = when {
            endTime.isAfter(startTime) -> Duration.between(startTime, endTime).toMinutes() / 60.0
            endTime.isBefore(startTime) -> (Duration.between(startTime, endTime).toMinutes() + 24 * 60) / 60.0
            else -> 0.0
        }

    val selectedRoute: RouteEntity?
        get() = routes.firstOrNull { it.id == routeId }
}

class EditTimeEntryViewModel(
    private val repository: TimeTrackingRepository,
    entryId: Long?
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private val _uiState = MutableStateFlow(EditTimeEntryUiState(entryId = entryId))
    val uiState: StateFlow<EditTimeEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRoutes().collect { routes ->
                _uiState.value = _uiState.value.copy(routes = routes)
            }
        }
        if (entryId != null) {
            viewModelScope.launch {
                repository.getEntryById(entryId)?.let { existing ->
                    val start = Instant.ofEpochMilli(existing.startEpochMillis).atZone(zone)
                    val end = Instant.ofEpochMilli(existing.endEpochMillis).atZone(zone)
                    _uiState.value = _uiState.value.copy(
                        createdAtEpochMillis = existing.createdAtEpochMillis,
                        date = start.toLocalDate(),
                        payType = existing.payType,
                        routeId = existing.routeId,
                        label = existing.label,
                        startTime = start.toLocalTime(),
                        endTime = end.toLocalTime(),
                        notes = existing.notes ?: ""
                    )
                }
            }
        }
    }

    fun setDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date, error = null)
    }

    fun setPayType(payType: PayType) {
        _uiState.value = _uiState.value.copy(payType = payType, error = null)
    }

    fun setLabel(value: String) {
        _uiState.value = _uiState.value.copy(label = value, routeId = null, error = null)
    }

    fun setStartTime(value: LocalTime) {
        _uiState.value = _uiState.value.copy(startTime = value, error = null)
    }

    fun setEndTime(value: LocalTime) {
        _uiState.value = _uiState.value.copy(endTime = value, error = null)
    }

    fun setNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    /** Route quick-input: prefills the label. Evaluated hours/miles are pulled from the route itself at save time. */
    fun selectRoute(route: RouteEntity) {
        _uiState.value = _uiState.value.copy(
            routeId = route.id,
            label = "${route.routeNumber} - ${route.cityName}",
            error = null
        )
    }

    fun save() {
        val state = _uiState.value
        if (state.label.isBlank()) {
            _uiState.value = state.copy(error = "Enter a route or job label")
            return
        }
        if (state.actualHours <= 0) {
            _uiState.value = state.copy(error = "End time must be after start time")
            return
        }

        val (evaluatedHours, evaluatedMiles) = when (state.payType) {
            PayType.HOURLY -> null to null
            PayType.EVALUATION -> {
                val route = state.selectedRoute
                if (route == null) {
                    _uiState.value = state.copy(error = "Select a route to pull evaluated hours from")
                    return
                }
                route.evaluatedHours to route.evaluatedMiles
            }
        }

        val startInstant = state.date.atTime(state.startTime).atZone(zone).toInstant()
        val endDate = if (state.endTime.isBefore(state.startTime)) state.date.plusDays(1) else state.date
        val endInstant = endDate.atTime(state.endTime).atZone(zone).toInstant()

        val entry = TimeEntryEntity(
            id = state.entryId ?: 0,
            startEpochMillis = startInstant.toEpochMilli(),
            endEpochMillis = endInstant.toEpochMilli(),
            payType = state.payType,
            routeId = state.routeId,
            label = state.label.trim(),
            evaluatedHours = evaluatedHours,
            evaluatedMiles = evaluatedMiles,
            notes = state.notes.trim().ifBlank { null },
            createdAtEpochMillis = state.createdAtEpochMillis
        )

        viewModelScope.launch {
            repository.saveEntry(entry)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun delete() {
        val id = _uiState.value.entryId ?: return
        viewModelScope.launch {
            repository.getEntryById(id)?.let { repository.deleteEntry(it) }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    class Factory(
        private val repository: TimeTrackingRepository,
        private val entryId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditTimeEntryViewModel(repository, entryId) as T
    }
}

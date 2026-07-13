package com.isaacshub.app.timetracking.ui.editroute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.ScheduleType
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.timetracking.ui.formatNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditRouteUiState(
    val routeId: Long? = null,
    val routeNumber: String = "",
    val cityName: String = "",
    val evaluatedHours: String = "",
    val evaluatedMiles: String = "",
    val scheduleType: ScheduleType = ScheduleType.NONE,
    val scheduleAnchor: LocalDate = LocalDate.now(),
    val error: String? = null,
    val saved: Boolean = false
) {
    val needsAnchor: Boolean get() = scheduleType == ScheduleType.WEEKLY || scheduleType == ScheduleType.BIWEEKLY
}

class EditRouteViewModel(
    private val repository: TimeTrackingRepository,
    routeId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRouteUiState(routeId = routeId))
    val uiState: StateFlow<EditRouteUiState> = _uiState.asStateFlow()

    init {
        if (routeId != null) {
            viewModelScope.launch {
                repository.getRouteById(routeId)?.let { existing ->
                    _uiState.value = _uiState.value.copy(
                        routeNumber = existing.routeNumber,
                        cityName = existing.cityName,
                        evaluatedHours = formatNumber(existing.evaluatedHours),
                        evaluatedMiles = formatNumber(existing.evaluatedMiles),
                        scheduleType = existing.scheduleType,
                        scheduleAnchor = existing.scheduleAnchorEpochDay?.let(LocalDate::ofEpochDay)
                            ?: LocalDate.now()
                    )
                }
            }
        }
    }

    fun setRouteNumber(value: String) {
        _uiState.value = _uiState.value.copy(routeNumber = value, error = null)
    }

    fun setCityName(value: String) {
        _uiState.value = _uiState.value.copy(cityName = value, error = null)
    }

    fun setEvaluatedHours(value: String) {
        _uiState.value = _uiState.value.copy(evaluatedHours = value, error = null)
    }

    fun setEvaluatedMiles(value: String) {
        _uiState.value = _uiState.value.copy(evaluatedMiles = value, error = null)
    }

    fun setScheduleType(value: ScheduleType) {
        _uiState.value = _uiState.value.copy(scheduleType = value, error = null)
    }

    fun setScheduleAnchor(value: LocalDate) {
        _uiState.value = _uiState.value.copy(scheduleAnchor = value, error = null)
    }

    fun save() {
        val state = _uiState.value
        val hours = state.evaluatedHours.toDoubleOrNull()
        val miles = state.evaluatedMiles.toDoubleOrNull()
        when {
            state.routeNumber.isBlank() -> _uiState.value = state.copy(error = "Enter a route number")
            state.cityName.isBlank() -> _uiState.value = state.copy(error = "Enter a city name")
            hours == null || hours < 0 -> _uiState.value = state.copy(error = "Enter valid evaluated hours")
            miles == null || miles < 0 -> _uiState.value = state.copy(error = "Enter valid evaluated miles")
            else -> viewModelScope.launch {
                repository.saveRoute(
                    RouteEntity(
                        id = state.routeId ?: 0,
                        routeNumber = state.routeNumber.trim(),
                        cityName = state.cityName.trim(),
                        evaluatedHours = hours,
                        evaluatedMiles = miles,
                        scheduleType = state.scheduleType,
                        scheduleAnchorEpochDay = if (state.needsAnchor) state.scheduleAnchor.toEpochDay() else null
                    )
                )
                _uiState.value = _uiState.value.copy(saved = true)
            }
        }
    }

    fun delete() {
        val id = _uiState.value.routeId ?: return
        viewModelScope.launch {
            repository.getRouteById(id)?.let { repository.deleteRoute(it) }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    class Factory(
        private val repository: TimeTrackingRepository,
        private val routeId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditRouteViewModel(repository, routeId) as T
    }
}

package com.isaacshub.app.routehelper.ui.builder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.isaacshub.app.routehelper.data.CandidateAddressEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.StopSide
import com.isaacshub.app.routehelper.domain.nearestAddresses
import com.isaacshub.app.routehelper.location.liveLocationFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RouteBuilderUiState(
    val currentLocation: GeoPoint? = null,
    val nearestAddresses: List<CandidateAddressEntity> = emptyList(),
    val routedStops: List<RoutedStopEntity> = emptyList(),
    val canUndo: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class RouteBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<App>().routeHelperRepository
    private val routeIdFlow = MutableStateFlow<Long?>(null)
    private val locationFlow = MutableStateFlow<GeoPoint?>(null)
    private var locationStarted = false

    /** Safe to call every time the screen recomposes - only actually starts tracking/observing once per route. */
    fun start(routeId: Long) {
        if (routeIdFlow.value != routeId) {
            routeIdFlow.value = routeId
        }
        if (!locationStarted) {
            locationStarted = true
            viewModelScope.launch {
                liveLocationFlow(getApplication()).collect { locationFlow.value = it }
            }
        }
    }

    private val candidatesFlow = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeUnroutedCandidates(id)
    }

    private val stopsFlow = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeStops(id)
    }

    val uiState: StateFlow<RouteBuilderUiState> = combine(
        locationFlow,
        candidatesFlow,
        stopsFlow
    ) { location, candidates, stops ->
        val nearest = if (location == null) {
            emptyList()
        } else {
            val domainCandidates = candidates.map {
                com.isaacshub.app.routehelper.domain.CandidateAddress(it.id, it.label, GeoPoint(it.latitude, it.longitude))
            }
            val byId = candidates.associateBy { it.id }
            nearestAddresses(location, domainCandidates, count = 5).mapNotNull { byId[it.id] }
        }
        RouteBuilderUiState(
            currentLocation = location,
            nearestAddresses = nearest,
            routedStops = stops,
            canUndo = stops.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RouteBuilderUiState())

    /** One tap: adds [candidate] as the next stop at wherever the driver is right now, with [side]'s canned note (or none). */
    fun routeStop(candidate: CandidateAddressEntity, side: StopSide) {
        val location = locationFlow.value ?: return
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch {
            repository.addStop(routeId, candidate.id, candidate.label, side.note, location)
        }
    }

    fun undo() {
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch { repository.undoLastStop(routeId) }
    }
}

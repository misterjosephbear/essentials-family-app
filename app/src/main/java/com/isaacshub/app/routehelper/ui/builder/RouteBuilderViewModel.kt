package com.isaacshub.app.routehelper.ui.builder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.isaacshub.app.routehelper.data.CandidateAddressEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity
import com.isaacshub.app.routehelper.domain.CandidateAddress
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.LocationSample
import com.isaacshub.app.routehelper.domain.StopSide
import com.isaacshub.app.routehelper.domain.nearestAddresses
import com.isaacshub.app.routehelper.domain.stickyNearestSlots
import com.isaacshub.app.routehelper.location.liveLocationFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SLOT_COUNT = 5

/** A one-tap button was pressed while still moving; it plants at wherever the driver ends up stopping. */
private data class PendingStop(val candidate: CandidateAddressEntity, val side: StopSide)

data class RouteBuilderUiState(
    val currentLocation: GeoPoint? = null,
    /** Always [SLOT_COUNT] entries, one per fixed on-screen position; null means that slot is empty. */
    val nearestAddressSlots: List<CandidateAddressEntity?> = List(SLOT_COUNT) { null },
    val pendingCandidateIds: Set<Long> = emptySet(),
    val routedStops: List<RoutedStopEntity> = emptyList(),
    val canUndo: Boolean = false,
    val routeZip: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class RouteBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<App>().routeHelperRepository
    private val routeIdFlow = MutableStateFlow<Long?>(null)
    private val locationFlow = MutableStateFlow<LocationSample?>(null)
    private val pendingStopsFlow = MutableStateFlow<List<PendingStop>>(emptyList())
    private var locationStarted = false

    /** Safe to call every time the screen recomposes - only actually starts tracking/observing once per route. */
    fun start(routeId: Long) {
        if (routeIdFlow.value != routeId) {
            routeIdFlow.value = routeId
        }
        if (!locationStarted) {
            locationStarted = true
            viewModelScope.launch {
                liveLocationFlow(getApplication()).collect { sample ->
                    locationFlow.value = sample
                    if (sample.isStopped()) drainPendingStops(sample.point)
                }
            }
        }
    }

    private val candidatesFlow = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeUnroutedCandidates(id)
    }

    private val stopsFlow = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeStops(id)
    }

    private val routeZipFlow: Flow<String?> = routeIdFlow.flatMapLatest { id ->
        kotlinx.coroutines.flow.flow {
            if (id == null) {
                emit(null)
            } else {
                val route = repository.getRoute(id)
                emit(route?.zipCode)
            }
        }
    }

    /** The current top [SLOT_COUNT] nearest unrouted candidates, re-ranked on every location/candidate change. */
    private val nearestRawFlow: Flow<List<CandidateAddress>> = combine(locationFlow, candidatesFlow) { sample, candidates ->
        val location = sample?.point
        if (location == null) {
            emptyList()
        } else {
            val domainCandidates = candidates.map {
                CandidateAddress(it.id, it.label, GeoPoint(it.latitude, it.longitude))
            }
            nearestAddresses(location, domainCandidates, count = SLOT_COUNT)
        }
    }

    /**
     * [nearestRawFlow] re-ranked, but pinned to fixed slots that only turn over when a candidate is
     * routed or truly falls out of the nearest set - see [stickyNearestSlots]. This is what keeps the
     * on-screen list from reshuffling under the driver's thumb as distances change while moving.
     */
    private val stickySlotIdsFlow: Flow<List<Long?>> = nearestRawFlow.scan(List(SLOT_COUNT) { null }) { previousSlots, nearest ->
        stickyNearestSlots(previousSlots, nearest)
    }

    val uiState: StateFlow<RouteBuilderUiState> = combine(
        locationFlow,
        stickySlotIdsFlow,
        candidatesFlow,
        stopsFlow,
        pendingStopsFlow,
        routeZipFlow
    ) { values: Array<Any?> ->
        val sample = values[0] as LocationSample?
        val slotIds = values[1] as List<Long?>
        val candidates = values[2] as List<CandidateAddressEntity>
        val stops = values[3] as List<RoutedStopEntity>
        val pending = values[4] as List<PendingStop>
        val routeZip = values[5] as String?
        val byId = candidates.associateBy { it.id }
        RouteBuilderUiState(
            currentLocation = sample?.point,
            nearestAddressSlots = slotIds.map { id -> id?.let { byId[it] } },
            pendingCandidateIds = pending.map { it.candidate.id }.toSet(),
            routedStops = stops,
            canUndo = stops.isNotEmpty(),
            routeZip = routeZip
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RouteBuilderUiState())

    /**
     * One tap: if the driver is already fully stopped, routes [candidate] immediately at the current
     * location. Otherwise the tap is queued - it plants at wherever the driver next comes to a full
     * stop, so a button can be pressed while still rolling up to the mailbox without misplacing the pin.
     */
    fun routeStop(candidate: CandidateAddressEntity, side: StopSide) {
        val sample = locationFlow.value ?: return
        val routeId = routeIdFlow.value ?: return
        if (sample.isStopped()) {
            viewModelScope.launch {
                repository.addStop(routeId, candidate.id, candidate.label, side.note, sample.point)
            }
        } else {
            pendingStopsFlow.value = pendingStopsFlow.value + PendingStop(candidate, side)
        }
    }

    /** Plants every queued tap at [location], in the order they were pressed, then clears the queue. */
    private fun drainPendingStops(location: GeoPoint) {
        val queued = pendingStopsFlow.value
        if (queued.isEmpty()) return
        val routeId = routeIdFlow.value ?: return
        pendingStopsFlow.value = emptyList()
        viewModelScope.launch {
            queued.forEach { pending ->
                repository.addStop(routeId, pending.candidate.id, pending.candidate.label, pending.side.note, location)
            }
        }
    }

    fun undo() {
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch { repository.undoLastStop(routeId) }
    }

    fun addScannedStop(resolved: com.isaacshub.app.routehelper.ui.player.ResolvedMailStop) {
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch {
            repository.addStop(
                routeId = routeId,
                candidateId = null,  // Scanned stops don't have a candidate ID
                addressLabel = resolved.addressLabel,
                note = resolved.recipientLastName,
                location = resolved.location
            )
        }
    }
}

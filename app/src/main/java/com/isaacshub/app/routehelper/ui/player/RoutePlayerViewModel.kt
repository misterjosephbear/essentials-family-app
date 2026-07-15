package com.isaacshub.app.routehelper.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.routehelper.data.RoutedStopEntity
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.LocationSample
import com.isaacshub.app.routehelper.domain.advanceToNextStop
import com.isaacshub.app.routehelper.domain.resolveMapBearing
import com.isaacshub.app.routehelper.location.liveLocationFlow
import com.isaacshub.app.routehelper.network.RouteDirectionsFetcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutePlayerUiState(
    val currentLocation: GeoPoint? = null,
    val mapBearingDegrees: Float = 0f,
    val stops: List<RoutedStopEntity> = emptyList(),
    /** Index into [stops] of the stop the driver is heading to next; null once every stop has been reached. */
    val nextStopIndex: Int? = null,
    /**
     * The road-following path through [stops], fetched from OSRM. Null while it's still loading or if
     * the fetch failed (no signal, etc.) - the map falls back to a straight line between stops then.
     */
    val roadRoutePoints: List<GeoPoint>? = null
)

/**
 * Drives the route player: replays a route already built in [com.isaacshub.app.routehelper.ui.builder.RouteBuilderScreen]
 * as a live GPS guide, tracking which of its already-recorded stops is next and which way the map should
 * face. It never writes new stops - unlike the builder, this is read-only playback of a finished route.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutePlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<App>().routeHelperRepository
    private val directionsFetcher = RouteDirectionsFetcher()
    private val routeIdFlow = MutableStateFlow<Long?>(null)
    private val locationFlow = MutableStateFlow<LocationSample?>(null)
    private var locationStarted = false

    /** Safe to call every time the screen recomposes - only actually starts tracking/observing once per route. */
    fun start(routeId: Long) {
        if (routeIdFlow.value != routeId) {
            routeIdFlow.value = routeId
        }
        if (!locationStarted) {
            locationStarted = true
            viewModelScope.launch {
                liveLocationFlow(getApplication()).collect { sample -> locationFlow.value = sample }
            }
        }
    }

    private val stopsFlow: Flow<List<RoutedStopEntity>> = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeStops(id)
    }

    /** Which stop the driver is heading to, advancing whenever they come within arrival range of the current one. */
    private val nextStopIndexFlow: Flow<Int> = locationFlow.combine(stopsFlow) { sample, stops -> sample to stops }
        .scan(0) { previousIndex, (sample, stops) ->
            val location = sample?.point
            val points = stops.map { GeoPoint(it.latitude, it.longitude) }
            if (location == null) previousIndex.coerceAtMost(points.size) else advanceToNextStop(location, points, previousIndex)
        }

    private val bearingFlow: Flow<Float> = locationFlow.scan(0f) { previousBearing, sample ->
        sample?.let { resolveMapBearing(it, previousBearing) } ?: previousBearing
    }

    /** Re-fetched whenever the stop list itself changes, not on every location tick. Emits null first so the rest of the screen doesn't stall waiting on the network. */
    private val roadRouteFlow: Flow<List<GeoPoint>?> = stopsFlow.flatMapLatest { stops ->
        flow {
            emit(null)
            if (stops.size >= 2) {
                emit(directionsFetcher.fetchDrivingRoute(stops.map { GeoPoint(it.latitude, it.longitude) }))
            }
        }
    }

    val uiState: StateFlow<RoutePlayerUiState> = combine(
        locationFlow,
        stopsFlow,
        nextStopIndexFlow,
        bearingFlow,
        roadRouteFlow
    ) { sample, stops, nextIndex, bearing, roadRoute ->
        RoutePlayerUiState(
            currentLocation = sample?.point,
            mapBearingDegrees = bearing,
            stops = stops,
            nextStopIndex = nextIndex.takeIf { it < stops.size },
            roadRoutePoints = roadRoute
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutePlayerUiState())
}

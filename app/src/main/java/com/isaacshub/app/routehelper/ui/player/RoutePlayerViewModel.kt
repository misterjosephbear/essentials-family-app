package com.isaacshub.app.routehelper.ui.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.routehelper.data.CachedRoadRouteEntity
import com.isaacshub.app.routehelper.data.PackageEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.LocationSample
import com.isaacshub.app.routehelper.domain.advanceToNextStop
import com.isaacshub.app.routehelper.domain.distanceMeters
import com.isaacshub.app.routehelper.domain.resolveMapBearing
import com.isaacshub.app.routehelper.domain.STOP_ARRIVAL_RADIUS_METERS
import com.isaacshub.app.routehelper.location.liveLocationFlow
import com.isaacshub.app.routehelper.network.RouteDirectionsFetcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "RoutePlayerViewModel"

/** How close stops must be to be considered a cluster (15 meters ≈ 50 feet). */
private const val STOP_CLUSTER_RADIUS_METERS = 15.0

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
    val roadRoutePoints: List<GeoPoint>? = null,
    /** Debug info about road route status */
    val roadRouteDebugInfo: String = "",
    /** ZIP code for the current route - used to validate envelope scans */
    val routeZip: String? = null,
    /** Current road name from reverse geocoding - used to validate envelope scans */
    val currentRoadName: String? = null,
    /** True if the driver is currently stopped at a stop (within arrival radius) */
    val isAtStop: Boolean = false,
    /** List of stops in the current cluster (next stops that are close together) */
    val clusterStops: List<RoutedStopEntity> = emptyList(),
    /** Map of stop ID to package count for that stop */
    val packageCountsByStop: Map<Long, Int> = emptyMap(),
    /** Map of stop ID to first package address for that stop (for "next package" display) */
    val nextPackageAddressByStop: Map<Long, String> = emptyMap(),
    /** Address of the next package stop (could be many stops ahead), null if no more packages */
    val nextPackageAddress: String? = null,
    /** Number of stops until the next package (0 if at package stop, null if no more packages) */
    val stopsUntilNextPackage: Int? = null
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
    private val rawLocationFlow = MutableStateFlow<LocationSample?>(null)
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
                liveLocationFlow(getApplication()).collect { sample ->
                    rawLocationFlow.value = sample
                    // Smooth location updates - gentle interpolation to reduce GPS jitter
                    val current = locationFlow.value
                    if (current != null && sample != null) {
                        // Use 50/50 blend for smoother but still responsive movement
                        val smoothed = LocationSample(
                            point = GeoPoint(
                                latitude = current.point.latitude * 0.5 + sample.point.latitude * 0.5,
                                longitude = current.point.longitude * 0.5 + sample.point.longitude * 0.5
                            ),
                            speedMetersPerSecond = sample.speedMetersPerSecond,
                            bearingDegrees = sample.bearingDegrees
                        )
                        locationFlow.value = smoothed
                    } else {
                        locationFlow.value = sample
                    }
                }
            }
        }
    }

    private val stopsFlow: Flow<List<RoutedStopEntity>> = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeStops(id)
    }

    private val packagesFlow: Flow<List<PackageEntity>> = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeUndeliveredPackages(id)
    }

    private val routeZipFlow: Flow<String?> = routeIdFlow.flatMapLatest { id ->
        flow {
            if (id == null) {
                emit(null)
            } else {
                val route = repository.getRoute(id)
                emit(route?.zipCode)
            }
        }
    }

    // TODO: Implement reverse geocoding for current road name
    // For now, return null - will add Nominatim reverse geocoding in next iteration
    private val currentRoadNameFlow: Flow<String?> = flowOf(null)

    /** Which stop the driver is heading to, advancing whenever they come within arrival range of the current one. */
    private data class StopAdvanceState(val index: Int, val previousLocation: GeoPoint?)

    private val nextStopIndexFlow: Flow<Int> = locationFlow.combine(stopsFlow) { sample, stops -> sample to stops }
        .scan(StopAdvanceState(0, null)) { state, (sample, stops) ->
            val location = sample?.point
            val speed = sample?.speedMetersPerSecond ?: 0f
            val points = stops.map { GeoPoint(it.latitude, it.longitude) }
            if (location == null) {
                StopAdvanceState(state.index.coerceAtMost(points.size), null)
            } else {
                val newIndex = advanceToNextStop(location, state.previousLocation, points, state.index, speed)
                StopAdvanceState(newIndex, location)
            }
        }
        .map { it.index }

    private val bearingFlow: Flow<Float> = locationFlow.scan(0f) { previousBearing, sample ->
        sample?.let { resolveMapBearing(it, previousBearing) } ?: previousBearing
    }

    /**
     * Road route with offline caching: checks cache first, then fetches online if needed.
     * Emits cached route immediately if available, then fetches fresh route in background.
     *
     * Uses distinctUntilChanged to prevent re-fetching when stops emit multiple times with same data.
     */
    private data class RoadRouteResult(val points: List<GeoPoint>?, val debugInfo: String)

    private val roadRouteFlow: Flow<RoadRouteResult> = combine(routeIdFlow, stopsFlow) { routeId, stops -> routeId to stops }
        .distinctUntilChanged { old, new ->
            // Only re-fetch if route ID or stop count/positions actually changed
            old.first == new.first && old.second.size == new.second.size &&
                old.second.zip(new.second).all { (a, b) ->
                    a.latitude == b.latitude && a.longitude == b.longitude
                }
        }
        .flatMapLatest { (routeId, stops) ->
            flow {
                if (routeId == null || stops.size < 2) {
                    Log.d(TAG, "Skipping road route fetch: routeId=$routeId, stops=${stops.size}")
                    emit(RoadRouteResult(null, "No route or < 2 stops"))
                    return@flow
                }

                Log.d(TAG, "Fetching road route for ${stops.size} stops")

                // Try cache first for instant offline display
                val cached = repository.getCachedRoadRoute(routeId)
                if (cached != null) {
                    val points = parsePolylineJson(cached.polylineJson)
                    Log.d(TAG, "Using cached road route with ${points.size} points")
                    emit(RoadRouteResult(points, "Cached: ${points.size} pts"))
                } else {
                    Log.d(TAG, "No cached road route, emitting null while fetching")
                    emit(RoadRouteResult(null, "Fetching..."))  // No cache, show straight lines while fetching
                }

                // Fetch fresh route online and cache it
                val waypoints = stops.map { GeoPoint(it.latitude, it.longitude) }
                Log.d(TAG, "Fetching from OSRM with waypoints: ${waypoints.joinToString { "(${it.latitude},${it.longitude})" }}")

                try {
                    val fetched = directionsFetcher.fetchDrivingRoute(waypoints)
                    if (fetched != null) {
                        Log.d(TAG, "OSRM fetch succeeded with ${fetched.size} points, caching...")
                        // Cache for offline use
                        repository.cacheRoadRoute(routeId, fetched)
                        emit(RoadRouteResult(fetched, "OSRM: ${fetched.size} pts"))
                    } else {
                        Log.w(TAG, "OSRM fetch returned null - check logs for details")
                        emit(RoadRouteResult(cached?.let { parsePolylineJson(it.polylineJson) }, "OSRM failed (${stops.size} stops)"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OSRM fetch threw exception", e)
                    emit(RoadRouteResult(cached?.let { parsePolylineJson(it.polylineJson) }, "OSRM error: ${e.message?.take(30)}"))
                }
            }
        }

    /**
     * Slots a stop scanned off a mail piece in right after wherever the driver last was - i.e. just
     * before the stop they're currently heading to (or at the end, if every stop's already been hit).
     */
    fun addScannedStop(resolved: ResolvedMailStop) {
        val routeId = routeIdFlow.value ?: return
        val state = uiState.value
        val beforeStopId = state.nextStopIndex?.let { state.stops.getOrNull(it)?.id }
        viewModelScope.launch {
            repository.insertStopBefore(routeId, beforeStopId, resolved.addressLabel, resolved.recipientLastName, resolved.location)
            // Invalidate cached road route since stops changed - will trigger fresh fetch
            repository.deleteCachedRoadRoute(routeId)
        }
    }

    val uiState: StateFlow<RoutePlayerUiState> = combine(
        locationFlow,
        stopsFlow,
        nextStopIndexFlow,
        bearingFlow,
        roadRouteFlow,
        routeZipFlow,
        currentRoadNameFlow,
        packagesFlow
    ) { values: Array<Any?> ->
        val sample = values[0] as LocationSample?
        val stops = values[1] as List<RoutedStopEntity>
        val nextIndex = values[2] as Int
        val bearing = values[3] as Float
        val roadRoute = values[4] as RoadRouteResult
        val routeZip = values[5] as String?
        val currentRoadName = values[6] as String?
        val packages = values[7] as List<PackageEntity>

        // Calculate if driver is currently at a stop
        val currentLocation = sample?.point
        val isAtStop = if (currentLocation != null && nextIndex < stops.size) {
            val nextStop = stops[nextIndex]
            val nextStopPoint = GeoPoint(nextStop.latitude, nextStop.longitude)
            distanceMeters(currentLocation, nextStopPoint) < STOP_ARRIVAL_RADIUS_METERS
        } else {
            false
        }

        // Calculate cluster of nearby stops
        val clusterStops = if (currentLocation != null && nextIndex < stops.size) {
            val cluster = mutableListOf<RoutedStopEntity>()
            cluster.add(stops[nextIndex])
            // Find consecutive stops within cluster radius
            for (i in (nextIndex + 1) until stops.size) {
                val prevStop = stops[i - 1]
                val currentStop = stops[i]
                val prevPoint = GeoPoint(prevStop.latitude, prevStop.longitude)
                val currentPoint = GeoPoint(currentStop.latitude, currentStop.longitude)
                if (distanceMeters(prevPoint, currentPoint) < STOP_CLUSTER_RADIUS_METERS) {
                    cluster.add(currentStop)
                } else {
                    break  // Stop clustering when gap is too large
                }
            }
            cluster
        } else {
            emptyList()
        }

        // Calculate package counts and next package address for each stop
        val packageCountsByStop = mutableMapOf<Long, Int>()
        val nextPackageAddressByStop = mutableMapOf<Long, String>()

        packages.forEach { pkg ->
            pkg.routedStopId?.let { stopId ->
                // Increment count
                packageCountsByStop[stopId] = (packageCountsByStop[stopId] ?: 0) + 1

                // Store first package address for this stop
                if (!nextPackageAddressByStop.containsKey(stopId)) {
                    nextPackageAddressByStop[stopId] = pkg.addressLabel
                }
            }
        }

        // Find the next package location (could be many stops ahead)
        var nextPackageAddress: String? = null
        var stopsUntilNextPackage: Int? = null

        if (nextIndex < stops.size) {
            // Look through remaining stops to find the next one with packages
            for (i in nextIndex until stops.size) {
                val stop = stops[i]
                if (packageCountsByStop[stop.id] ?: 0 > 0) {
                    nextPackageAddress = nextPackageAddressByStop[stop.id]
                    stopsUntilNextPackage = i - nextIndex
                    break
                }
            }
        }

        RoutePlayerUiState(
            currentLocation = currentLocation,
            mapBearingDegrees = bearing,
            stops = stops,
            nextStopIndex = nextIndex.takeIf { it < stops.size },
            roadRoutePoints = roadRoute.points,
            roadRouteDebugInfo = roadRoute.debugInfo,
            routeZip = routeZip,
            currentRoadName = currentRoadName,
            isAtStop = isAtStop,
            clusterStops = clusterStops,
            packageCountsByStop = packageCountsByStop,
            nextPackageAddressByStop = nextPackageAddressByStop,
            nextPackageAddress = nextPackageAddress,
            stopsUntilNextPackage = stopsUntilNextPackage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutePlayerUiState())

    private fun serializePolylineToJson(points: List<GeoPoint>): String {
        val array = JSONArray()
        points.forEach { point ->
            array.put(JSONArray().put(point.latitude).put(point.longitude))
        }
        return array.toString()
    }

    private fun parsePolylineJson(json: String): List<GeoPoint> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val pair = array.getJSONArray(i)
            GeoPoint(latitude = pair.getDouble(0), longitude = pair.getDouble(1))
        }
    }

    /** Get package count for a specific stop */
    suspend fun getPackageCountForStop(stopId: Long): Int {
        val routeId = routeIdFlow.value ?: return 0
        return repository.getUndeliveredPackageCountForStop(routeId, stopId)
    }

    /** Add a scanned package with matched stop ID */
    fun addPackage(trackingNumber: String, addressLabel: String, routedStopId: Long?) {
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch {
            if (routedStopId != null) {
                // Package already matched to a stop during scanning - save with stop ID
                repository.addPackageWithStop(routeId, trackingNumber, addressLabel, routedStopId)
            } else {
                // Package not matched yet - save and then try to match
                repository.addPackage(routeId, trackingNumber, addressLabel)
                repository.matchPackagesToStops(routeId)
            }
        }
    }

    /** Observe all packages for this route */
    fun observePackages() = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observePackages(id)
    }

    /** Observe all packages with sequence numbers for this route */
    fun observePackagesWithSequence() = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observePackagesWithSequence(id)
    }

    /** Delete a package */
    fun deletePackage(pkg: com.isaacshub.app.routehelper.data.PackageEntity) {
        viewModelScope.launch {
            repository.deletePackage(pkg)
        }
    }

    /** Observe sections for all packages - returns map of package ID to list of section names */
    fun observePackageSections() = routeIdFlow.flatMapLatest { routeId ->
        if (routeId == null) {
            flowOf(emptyMap())
        } else {
            flow {
                repository.observePackagesWithSequence(routeId).collect { packages ->
                    val sectionsMap = mutableMapOf<Long, List<String>>()
                    packages.forEach { pkg ->
                        if (pkg.routedStopId != null) {
                            val sections = repository.getSectionsForStop(routeId, pkg.routedStopId)
                            sectionsMap[pkg.id] = sections.map { it.name }
                        } else {
                            sectionsMap[pkg.id] = emptyList()
                        }
                    }
                    emit(sectionsMap)
                }
            }
        }
    }
}

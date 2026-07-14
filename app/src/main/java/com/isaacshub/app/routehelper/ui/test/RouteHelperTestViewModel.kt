package com.isaacshub.app.routehelper.ui.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.routehelper.domain.CandidateAddress
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.StopSide
import com.isaacshub.app.routehelper.domain.nearestAddresses
import com.isaacshub.app.routehelper.network.AddressFetchResult
import com.isaacshub.app.routehelper.network.OsmAddressFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A stop routed while faking a location in testing mode - kept purely in memory, never written to Room, so it never counts as a real route. */
data class TestStop(val candidateId: Long, val label: String, val note: String?, val location: GeoPoint)

data class RouteHelperTestUiState(
    val zipInput: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val mapCenter: GeoPoint? = null,
    val fakeLocation: GeoPoint? = null,
    val nearestAddresses: List<CandidateAddress> = emptyList(),
    val testStops: List<TestStop> = emptyList()
)

/**
 * Backs the Route Helper "Testing Mode" screen: pick a ZIP to zoom to, tap the map to fake being at
 * a location, and see what the real route builder would show there. Nothing here ever touches the
 * routes/candidates/stops tables - it's all held in this ViewModel and disappears when the screen
 * is left, so a test run can never pollute or get counted as a real route.
 */
class RouteHelperTestViewModel(application: Application) : AndroidViewModel(application) {

    private val addressFetcher = OsmAddressFetcher()

    private val _uiState = MutableStateFlow(RouteHelperTestUiState())
    val uiState: StateFlow<RouteHelperTestUiState> = _uiState

    private var allCandidates: List<CandidateAddress> = emptyList()
    private val routedIds = mutableSetOf<Long>()

    fun onZipInputChange(value: String) {
        _uiState.value = _uiState.value.copy(zipInput = value)
    }

    fun loadZip() {
        val zip = _uiState.value.zipInput.trim()
        if (zip.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter a ZIP code")
            return
        }
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val center = addressFetcher.geocodeZip(zip)
            if (center == null) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Couldn't find that ZIP code")
                return@launch
            }
            when (val result = addressFetcher.fetchAddressesForZip(zip)) {
                is AddressFetchResult.Success -> {
                    allCandidates = result.addresses.mapIndexed { index, address ->
                        CandidateAddress(index.toLong(), address.label, address.location)
                    }
                    routedIds.clear()
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        mapCenter = center,
                        fakeLocation = center,
                        testStops = emptyList(),
                        nearestAddresses = nearestUnrouted(center)
                    )
                }
                is AddressFetchResult.Failure -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = "Couldn't fetch addresses: ${result.reason}")
                }
            }
        }
    }

    /** Called when the driver taps a spot on the map to pretend they're standing there. */
    fun setFakeLocation(point: GeoPoint) {
        _uiState.value = _uiState.value.copy(fakeLocation = point, nearestAddresses = nearestUnrouted(point))
    }

    fun routeStop(candidate: CandidateAddress, side: StopSide) {
        val location = _uiState.value.fakeLocation ?: return
        routedIds += candidate.id
        val stop = TestStop(candidate.id, candidate.label, side.note, location)
        _uiState.value = _uiState.value.copy(
            testStops = _uiState.value.testStops + stop,
            nearestAddresses = nearestUnrouted(location)
        )
    }

    fun undo() {
        val state = _uiState.value
        val last = state.testStops.lastOrNull() ?: return
        routedIds -= last.candidateId
        val from = state.fakeLocation
        _uiState.value = state.copy(
            testStops = state.testStops.dropLast(1),
            nearestAddresses = if (from != null) nearestUnrouted(from) else state.nearestAddresses
        )
    }

    private fun nearestUnrouted(from: GeoPoint): List<CandidateAddress> =
        nearestAddresses(from, allCandidates.filter { it.id !in routedIds }, count = 5)
}

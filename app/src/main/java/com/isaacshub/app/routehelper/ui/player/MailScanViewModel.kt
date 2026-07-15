package com.isaacshub.app.routehelper.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.ScannedAddress
import com.isaacshub.app.routehelper.domain.parseScannedAddresses
import com.isaacshub.app.routehelper.network.NominatimGeocoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A scanned address once it's been geocoded to a plottable point and is ready to become a stop. */
data class ResolvedMailStop(val addressLabel: String, val recipientLastName: String?, val location: GeoPoint)

data class MailScanUiState(
    /** More than one entry means the OCR pass found multiple address-shaped blocks - show a chooser. */
    val candidates: List<ScannedAddress> = emptyList(),
    val resolving: Boolean = false,
    val error: String? = null
)

/**
 * Owns OCR-to-stop resolution for [MailScanScreen]: takes whatever text ML Kit recognized in a camera
 * frame, picks out address-shaped blocks, lets the caller choose when there's more than one, then
 * geocodes the chosen text to a point via Nominatim. The camera/analyzer plumbing lives in the screen -
 * this only ever sees recognized text.
 */
class MailScanViewModel(application: Application) : AndroidViewModel(application) {

    private val geocoder = NominatimGeocoder()

    private val _uiState = MutableStateFlow(MailScanUiState())
    val uiState: StateFlow<MailScanUiState> = _uiState.asStateFlow()

    private val _resolved = MutableStateFlow<ResolvedMailStop?>(null)
    val resolved: StateFlow<ResolvedMailStop?> = _resolved.asStateFlow()

    /** True once a candidate has been chosen and is being geocoded (or already resolved) - stops new frames from interrupting it. */
    private var committed = false

    /** Driver's current GPS location - used as fallback if geocoding fails. */
    private var fallbackLocation: GeoPoint? = null

    fun setFallbackLocation(location: GeoPoint) {
        fallbackLocation = location
    }

    /** Resets the scanner state for a new scan. Must be called when the scanner screen is opened. */
    fun reset() {
        committed = false
        _resolved.value = null
        _uiState.value = MailScanUiState()
    }

    /** Fed a live OCR result from every analyzed camera frame; ignored once a candidate is already being resolved. */
    fun onTextRecognized(text: String) {
        if (committed || _uiState.value.resolving) return
        val candidates = parseScannedAddresses(text)
        if (candidates.isEmpty()) return
        if (candidates.size == 1) {
            choose(candidates.first())
        } else {
            _uiState.value = _uiState.value.copy(candidates = candidates)
        }
    }

    fun choose(candidate: ScannedAddress) {
        if (committed) return
        committed = true
        // Always use the driver's current GPS location - this is where they're physically stopped at the mailbox
        val location = fallbackLocation
        if (location == null) {
            _uiState.value = MailScanUiState(error = "No GPS location available. Try scanning again.")
            committed = false
            return
        }
        // Immediately resolve with cached data - exit scanner screen right away, no blocking
        _resolved.value = ResolvedMailStop(candidate.addressText, candidate.recipientLastName, location)
        // Note: Any future geocoding/validation can happen in background after screen closes
    }

    fun dismissError() {
        _uiState.value = MailScanUiState()
    }
}

package com.isaacshub.app.routehelper.ui.player

import android.app.Application
import android.util.Log
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

private const val TAG = "MailScanViewModel"
private const val REQUIRED_STABLE_FRAMES = 3  // Must see same street number 3 times

/** A scanned address once it's been geocoded to a plottable point and is ready to become a stop. */
data class ResolvedMailStop(val addressLabel: String, val recipientLastName: String?, val location: GeoPoint)

data class MailScanUiState(
    /** More than one entry means the OCR pass found multiple address-shaped blocks - show a chooser. */
    val candidates: List<ScannedAddress> = emptyList(),
    val resolving: Boolean = false,
    val error: String? = null,
    val validationStatus: String? = null  // Shows validation progress to user
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

    /** Expected ZIP code for the current route - addresses must match this ZIP. */
    private var routeZip: String? = null

    /** Current road name from reverse geocoding - addresses must match this road. */
    private var currentRoadName: String? = null

    /** Tracks consecutive frames where the same street number was seen - used for stability validation. */
    private val streetNumberHistory = mutableListOf<String>()

    /** The location where the scan was initiated - this is where the stop will be plotted. */
    private var scanLocation: GeoPoint? = null

    fun setFallbackLocation(location: GeoPoint) {
        fallbackLocation = location
        // Capture scan location on first GPS update
        if (scanLocation == null) {
            scanLocation = location
        }
    }

    fun setRouteZip(zip: String) {
        routeZip = zip
        Log.d(TAG, "Route ZIP set to: $zip")
    }

    fun setCurrentRoadName(roadName: String) {
        currentRoadName = roadName
        Log.d(TAG, "Current road name set to: $roadName")
    }

    /** Resets the scanner state for a new scan. Must be called when the scanner screen is opened. */
    fun reset() {
        committed = false
        _resolved.value = null
        _uiState.value = MailScanUiState()
        streetNumberHistory.clear()
        scanLocation = null
    }

    /** Fed a live OCR result from every analyzed camera frame; ignored once a candidate is already being resolved. */
    fun onTextRecognized(text: String) {
        if (committed || _uiState.value.resolving) return

        val candidates = parseScannedAddresses(text)
        if (candidates.isEmpty()) return

        // If multiple candidates, show chooser dialog
        if (candidates.size > 1) {
            _uiState.value = _uiState.value.copy(candidates = candidates)
            return
        }

        // Single candidate - validate before committing
        val candidate = candidates.first()

        // VALIDATION STEP 1: Must have complete address (street, city, state, ZIP)
        if (candidate.zip == null || candidate.city == null || candidate.state == null) {
            Log.d(TAG, "Incomplete address - missing city/state/ZIP")
            return
        }

        // VALIDATION STEP 2: ZIP must match route ZIP
        if (routeZip != null && candidate.zip != routeZip) {
            Log.d(TAG, "ZIP mismatch: scanned ${candidate.zip}, expected $routeZip")
            _uiState.value = _uiState.value.copy(
                validationStatus = "Wrong ZIP (${candidate.zip}), need $routeZip"
            )
            return
        }

        // VALIDATION STEP 3: Road name must match current road (fuzzy match)
        if (currentRoadName != null && !roadsMatch(candidate.streetName, currentRoadName!!)) {
            Log.d(TAG, "Road mismatch: scanned ${candidate.streetName}, expected $currentRoadName")
            _uiState.value = _uiState.value.copy(
                validationStatus = "Wrong road (${candidate.streetName}), need $currentRoadName"
            )
            return
        }

        // VALIDATION STEP 4: Street number must be stable across multiple frames
        streetNumberHistory.add(candidate.streetNumber)
        if (streetNumberHistory.size > 5) {
            streetNumberHistory.removeAt(0)  // Keep only last 5 frames
        }

        val stableNumber = getStableStreetNumber()
        if (stableNumber == null) {
            val count = streetNumberHistory.count { it == candidate.streetNumber }
            Log.d(TAG, "Street number not stable yet: seen ${candidate.streetNumber} $count/$REQUIRED_STABLE_FRAMES times")
            _uiState.value = _uiState.value.copy(
                validationStatus = "Reading address... (${candidate.streetNumber} - $count/$REQUIRED_STABLE_FRAMES)"
            )
            return
        }

        // ALL VALIDATIONS PASSED - commit the address
        Log.d(TAG, "All validations passed for: ${candidate.addressText}")
        choose(candidate)
    }

    /**
     * Returns the street number if it appears in at least REQUIRED_STABLE_FRAMES of the recent history,
     * null otherwise.
     */
    private fun getStableStreetNumber(): String? {
        if (streetNumberHistory.size < REQUIRED_STABLE_FRAMES) return null

        // Find the most common street number in recent history
        val numberCounts = streetNumberHistory.groupingBy { it }.eachCount()
        val mostCommon = numberCounts.maxByOrNull { it.value }

        return if (mostCommon != null && mostCommon.value >= REQUIRED_STABLE_FRAMES) {
            mostCommon.key
        } else {
            null
        }
    }

    /**
     * Fuzzy matches two road names - handles common OCR errors and abbreviations.
     * Examples: "MAIN ST" matches "Main Street", "1ST AVE" matches "First Avenue"
     */
    private fun roadsMatch(scanned: String, current: String): Boolean {
        // Normalize both strings: uppercase, remove punctuation
        val s1 = scanned.uppercase().replace(Regex("[^A-Z0-9 ]"), "")
        val s2 = current.uppercase().replace(Regex("[^A-Z0-9 ]"), "")

        // Exact match after normalization
        if (s1 == s2) return true

        // Check if one contains the other (handles abbreviations)
        if (s1.contains(s2) || s2.contains(s1)) return true

        // Extract the core road name (without type suffix like ST, AVE, RD, etc.)
        val commonSuffixes = listOf("ST", "STREET", "AVE", "AVENUE", "RD", "ROAD", "DR", "DRIVE",
                                     "LN", "LANE", "CT", "COURT", "WAY", "BLVD", "BOULEVARD", "PL", "PLACE")
        var core1 = s1
        var core2 = s2
        for (suffix in commonSuffixes) {
            core1 = core1.removeSuffix(" $suffix")
            core2 = core2.removeSuffix(" $suffix")
        }

        // Compare cores
        return core1 == core2
    }

    fun choose(candidate: ScannedAddress) {
        if (committed) return
        committed = true
        // Use the scan location (where camera was opened) - NOT the current GPS after processing
        // This ensures the stop is plotted where the letter was scanned, even if driver kept moving
        val location = scanLocation ?: fallbackLocation
        if (location == null) {
            _uiState.value = MailScanUiState(error = "No GPS location available. Try scanning again.")
            committed = false
            return
        }
        Log.d(TAG, "Committing address: ${candidate.addressText} at location ($location)")
        // Immediately resolve - exit scanner screen right away, no blocking
        _resolved.value = ResolvedMailStop(candidate.addressText, candidate.recipientLastName, location)
    }

    fun dismissError() {
        _uiState.value = MailScanUiState()
    }
}

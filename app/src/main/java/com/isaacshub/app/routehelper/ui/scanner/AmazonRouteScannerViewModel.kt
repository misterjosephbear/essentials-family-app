package com.isaacshub.app.routehelper.ui.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.routehelper.data.CandidateAddressEntity
import com.isaacshub.app.routehelper.data.ScannedAddressData
import com.isaacshub.app.routehelper.util.findBestCandidateMatch
import com.isaacshub.app.routehelper.util.normalizeAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannedAddress(
    val addressLabel: String,
    val sequenceNumber: Int,  // 1-based display number (our auto-generated sequence)
    val scannerSequenceNumber: Int?,  // Sequence number from scanner screen (if detected)
    val matchedCandidateId: Long?,
    val isValid: Boolean,
    val expectedPackageCount: Int? = null  // Package count from direction sheets
)

data class AmazonScannerUiState(
    val scannedAddresses: List<ScannedAddress> = emptyList(),
    val candidateAddresses: List<CandidateAddressEntity> = emptyList(),
    val isLoading: Boolean = true,
    val lastScannedAddress: String? = null,
    val validationMessage: String? = null,
    val isCreatingStops: Boolean = false,
    val error: String? = null
)

class AmazonRouteScannerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = getApplication<App>().routeHelperRepository

    private val _uiState = MutableStateFlow(AmazonScannerUiState())
    val uiState: StateFlow<AmazonScannerUiState> = _uiState.asStateFlow()

    private var currentRouteId: Long? = null

    fun start(routeId: Long) {
        if (currentRouteId == routeId) return  // Already started
        currentRouteId = routeId
        loadCandidateAddresses(routeId)
    }

    private fun loadCandidateAddresses(routeId: Long) {
        viewModelScope.launch {
            try {
                val candidates = mutableListOf<CandidateAddressEntity>()
                repository.observeUnroutedCandidates(routeId).collect { list ->
                    candidates.clear()
                    candidates.addAll(list)
                    _uiState.update { it.copy(candidateAddresses = list, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to load addresses: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onAddressScanned(
        rawAddress: String,
        scannerSequenceNumber: Int?,
        expectedPackageCount: Int? = null,
        routeId: String? = null
    ) {
        val trimmedAddress = rawAddress.trim()
        if (trimmedAddress.isEmpty()) return

        // Check for duplicates
        val normalizedScanned = normalizeAddress(trimmedAddress)
        val isDuplicate = _uiState.value.scannedAddresses.any {address ->
            normalizeAddress(address.addressLabel) == normalizedScanned
        }

        if (isDuplicate) {
            _uiState.update {
                it.copy(
                    validationMessage = "⚠ Duplicate address - already scanned",
                    lastScannedAddress = trimmedAddress
                )
            }
            return
        }

        // Find best match among candidates
        val matchedCandidate = findBestCandidateMatch(
            trimmedAddress,
            _uiState.value.candidateAddresses,
            similarityThreshold = 0.6
        )

        val isValid = matchedCandidate != null
        val nextSequence = _uiState.value.scannedAddresses.size + 1

        // Validate scanner sequence if present
        val sequenceCheck = if (scannerSequenceNumber != null) {
            val expected = nextSequence
            if (scannerSequenceNumber == expected) {
                "✓ Seq #$scannerSequenceNumber matches"
            } else {
                "⚠ Seq #$scannerSequenceNumber (expected #$expected)"
            }
        } else {
            null
        }

        // Log sequence validation
        android.util.Log.d("AmazonScanner",
            "Scanned #$nextSequence: $trimmedAddress (scanner seq: $scannerSequenceNumber) - ${sequenceCheck ?: "no seq detected"}")

        val scannedAddress = ScannedAddress(
            addressLabel = trimmedAddress,
            sequenceNumber = nextSequence,
            scannerSequenceNumber = scannerSequenceNumber,
            matchedCandidateId = matchedCandidate?.id,
            isValid = isValid,
            expectedPackageCount = expectedPackageCount
        )

        val validationMsg = buildString {
            if (isValid) {
                append("✓ Matched: ${matchedCandidate?.label}")
            } else {
                append("⚠ No match found in ZIP code")
            }
            if (sequenceCheck != null) {
                append(" | $sequenceCheck")
            }
        }

        _uiState.update { state ->
            state.copy(
                scannedAddresses = state.scannedAddresses + scannedAddress,
                lastScannedAddress = trimmedAddress,
                validationMessage = validationMsg
            )
        }
    }

    fun removeAddress(address: ScannedAddress) {
        _uiState.update { state ->
            val updatedList = state.scannedAddresses.filter { it != address }
            // Renumber remaining addresses
            val renumbered = updatedList.mapIndexed { index, addr ->
                addr.copy(sequenceNumber = index + 1)
            }
            state.copy(scannedAddresses = renumbered)
        }
    }

    fun clearValidationMessage() {
        _uiState.update { it.copy(validationMessage = null) }
    }

    fun finishScanning(onComplete: () -> Unit) {
        val routeId = currentRouteId ?: return
        val addresses = _uiState.value.scannedAddresses
        if (addresses.isEmpty()) {
            _uiState.update { it.copy(error = "Scan at least one address before finishing") }
            return
        }

        _uiState.update { it.copy(isCreatingStops = true) }

        viewModelScope.launch {
            try {
                // Convert UI model to repository model
                val scannedData = addresses.map { addr ->
                    ScannedAddressData(
                        addressLabel = addr.addressLabel,
                        sequenceNumber = addr.sequenceNumber,
                        matchedCandidateId = addr.matchedCandidateId,
                        isValid = addr.isValid,
                        expectedPackageCount = addr.expectedPackageCount
                    )
                }

                repository.createStopsFromScannedAddresses(routeId, scannedData)
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to create stops: ${e.message}",
                        isCreatingStops = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

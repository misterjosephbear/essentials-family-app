package com.isaacshub.app.routehelper.ui.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

data class ScannedPackage(
    val trackingNumber: String,
    val addressLabel: String,
    val sequenceNumber: Int? = null
)

/**
 * Package scanner screen for scanning packages in the morning before route playback.
 * Extracts USPS tracking numbers and addresses from package labels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageScanScreen(
    routeId: Long,
    scannedPackages: List<ScannedPackage>,
    onPackageScanned: (ScannedPackage) -> Unit,
    onPackageDeleted: (ScannedPackage) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scan Packages (${scannedPackages.size})") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Check, contentDescription = "Done")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isScanning) {
                FloatingActionButton(
                    onClick = {
                        if (hasCameraPermission) {
                            isScanning = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(Icons.Filled.Camera, contentDescription = "Scan package")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isScanning && hasCameraPermission) {
                PackageCameraScanner(
                    routeId = routeId,
                    onPackageScanned = { pkg ->
                        onPackageScanned(pkg)
                        isScanning = false
                    },
                    onCancel = { isScanning = false }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (scannedPackages.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No packages scanned yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Tap the camera button to scan a package",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    items(scannedPackages) { pkg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Show sequence number if available
                                        pkg.sequenceNumber?.let { seq ->
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            ) {
                                                Text(
                                                    "#$seq",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            pkg.addressLabel,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(
                                        "Tracking: ${pkg.trackingNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                IconButton(onClick = { onPackageDeleted(pkg) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete package",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageCameraScanner(
    routeId: Long,
    onPackageScanned: (ScannedPackage) -> Unit,
    onCancel: () -> Unit
) {
    var useFrontCamera by remember { mutableStateOf(false) }
    var canScan by remember { mutableStateOf(true) }
    var lastScannedPackage by remember { mutableStateOf<ScannedPackage?>(null) }
    var sectionsForLastPackage by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastScannedTrackingNumber by remember { mutableStateOf<String?>(null) }

    // Access repository to get sections and route stops
    val context = LocalContext.current
    val app = context.applicationContext as com.isaacshub.app.App
    val repository = app.routeHelperRepository

    // Load route stops for address validation
    var routeStops by remember { mutableStateOf<List<com.isaacshub.app.routehelper.data.RoutedStopEntity>>(emptyList()) }
    LaunchedEffect(routeId) {
        routeStops = repository.getStopsOnce(routeId)
        android.util.Log.d("PackageScanner", "Loaded ${routeStops.size} stops for route $routeId")
    }

    // When a package is scanned, look up its sections and re-enable scanning after cooldown
    LaunchedEffect(lastScannedPackage) {
        lastScannedPackage?.let { pkg ->
            // Look up sections
            val stops = repository.getStopsOnce(routeId)
            val matchingStop = stops.find { stop ->
                stop.addressLabel.contains(pkg.addressLabel, ignoreCase = true) ||
                pkg.addressLabel.contains(stop.addressLabel, ignoreCase = true)
            }

            if (matchingStop != null) {
                val sections = repository.getSectionsForStop(routeId, matchingStop.id)
                sectionsForLastPackage = sections.map { it.name }
            } else {
                sectionsForLastPackage = emptyList()
            }

            // Re-enable scanning after 2 second cooldown
            kotlinx.coroutines.delay(2000)
            canScan = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scan Package") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch camera")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PackageCameraPreview(
                useFrontCamera = useFrontCamera,
                routeStops = routeStops,
                onPackageDetected = { pkg ->
                    // Only scan if we're not in cooldown and it's a new tracking number
                    if (canScan && pkg.trackingNumber != lastScannedTrackingNumber) {
                        canScan = false
                        lastScannedTrackingNumber = pkg.trackingNumber
                        lastScannedPackage = pkg
                        onPackageScanned(pkg)
                    }
                }
            )

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = if (canScan) {
                    CardDefaults.cardColors()
                } else {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        if (canScan) "Ready to scan" else "Processing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (canScan) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        if (canScan) {
                            "Point camera at package label"
                        } else {
                            "Wait 2 seconds between scans"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canScan) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Show last scanned package and sections at the bottom
            if (lastScannedPackage != null) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Last Scanned:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            lastScannedPackage!!.addressLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (sectionsForLastPackage.isNotEmpty()) {
                            Text(
                                "Sections: ${sectionsForLastPackage.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                "No sections assigned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageCameraPreview(
    useFrontCamera: Boolean,
    routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>,
    onPackageDetected: (ScannedPackage) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onPackageDetectedState = rememberUpdatedState(onPackageDetected)
    val routeStopsState = rememberUpdatedState(routeStops)

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }

                    val executor = Executors.newSingleThreadExecutor()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor) { imageProxy ->
                                processPackageImage(imageProxy, recognizer, routeStopsState.value, onPackageDetectedState.value)
                            }
                        }

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("PackageScanner", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        update = { previewView ->
            // Rebuild camera binding when camera selector changes
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val executor = Executors.newSingleThreadExecutor()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            processPackageImage(imageProxy, recognizer, routeStopsState.value, onPackageDetectedState.value)
                        }
                    }

                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PackageScanner", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

/**
 * Process an image frame to extract package tracking number and address.
 */
private fun processPackageImage(
    imageProxy: ImageProxy,
    recognizer: TextRecognizer,
    routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>,
    onPackageDetected: (ScannedPackage) -> Unit
) {
    @androidx.camera.core.ExperimentalGetImage
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                val trackingNumber = extractUSPSTrackingNumber(text)
                val address = extractAddress(text, routeStops)

                // Debug logging
                if (trackingNumber != null || address != null) {
                    android.util.Log.d("PackageScanner", "Found tracking: $trackingNumber, address: $address")
                    android.util.Log.d("PackageScanner", "Full OCR text:\n$text")
                }

                if (trackingNumber != null && address != null) {
                    onPackageDetected(ScannedPackage(trackingNumber, address))
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Extract USPS tracking number from OCR text.
 * USPS tracking numbers are typically 20-22 digits.
 */
private fun extractUSPSTrackingNumber(text: String): String? {
    // USPS tracking patterns:
    // - 20 digit: 9999 9999 9999 9999 9999
    // - 22 digit: 9999 9999 9999 9999 9999 99
    // - Also: 9400 1000 0000 0000 0000 00 (format varies)

    val trackingPatterns = listOf(
        Regex("""(\d{4}\s*\d{4}\s*\d{4}\s*\d{4}\s*\d{4}(\s*\d{2})?)"""),  // 20-22 digit with spaces
        Regex("""(\d{20,22})""")  // 20-22 digit continuous
    )

    for (pattern in trackingPatterns) {
        val match = pattern.find(text)
        if (match != null) {
            return match.value.replace(Regex("""\s+"""), "")  // Remove spaces
        }
    }

    return null
}

/**
 * Extract delivery address from OCR text with route validation.
 * On USPS packages, the delivery address is typically larger and centered.
 * We look for the largest/most prominent address pattern, avoiding the return address.
 *
 * Strategy:
 * 1. Find all lines that look like addresses (number + street)
 * 2. Filter out lines near "FROM:" or return address indicators
 * 3. Prefer addresses in middle section of text (delivery label is centered)
 * 4. Prefer longer addresses (delivery address typically more complete)
 * 5. VALIDATE against actual route stops - reject if not on route
 * 6. Return the most likely delivery address that matches a route stop
 */
private fun extractAddress(text: String, routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>): String? {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    // Address pattern: starts with a number, followed by street name
    val addressPattern = Regex("""^\d+\s+[A-Za-z].*""")

    // Return address indicators (typically in upper left)
    val returnAddressIndicators = listOf("from:", "return", "sender", "ship from")

    // Delivery indicators that suggest this is the delivery address
    val deliveryIndicators = listOf("deliver to", "ship to", "to:")

    // Find all potential addresses with metadata
    data class AddressCandidate(
        val index: Int,
        val text: String,
        val length: Int,
        val isNearDeliveryIndicator: Boolean,
        val isNearReturnIndicator: Boolean,
        val positionScore: Double  // Higher for addresses in middle of text
    )

    val candidates = mutableListOf<AddressCandidate>()

    for ((index, line) in lines.withIndex()) {
        if (addressPattern.matches(line)) {
            // Check context for indicators
            val contextLines = lines.subList(
                maxOf(0, index - 3),
                minOf(lines.size, index + 2)
            )

            val hasDeliveryIndicator = contextLines.any { contextLine ->
                deliveryIndicators.any { indicator ->
                    contextLine.lowercase().contains(indicator)
                }
            }

            val hasReturnIndicator = contextLines.any { contextLine ->
                returnAddressIndicators.any { indicator ->
                    contextLine.lowercase().contains(indicator)
                }
            }

            // Position score: prefer addresses in middle third of text
            val relativePosition = index.toDouble() / lines.size.coerceAtLeast(1)
            val positionScore = when {
                relativePosition < 0.25 -> 0.3  // Top quarter (likely return address)
                relativePosition > 0.75 -> 0.5  // Bottom quarter
                else -> 1.0  // Middle half (likely delivery address)
            }

            candidates.add(AddressCandidate(
                index = index,
                text = line,
                length = line.length,
                isNearDeliveryIndicator = hasDeliveryIndicator,
                isNearReturnIndicator = hasReturnIndicator,
                positionScore = positionScore
            ))
        }
    }

    if (candidates.isEmpty()) return null

    // Score each candidate
    val scoredCandidates = candidates.map { candidate ->
        var score = 0.0

        // Strong positive: near delivery indicator
        if (candidate.isNearDeliveryIndicator) score += 10.0

        // Strong negative: near return indicator
        if (candidate.isNearReturnIndicator) score -= 20.0

        // Prefer middle position
        score += candidate.positionScore * 5.0

        // Prefer longer addresses (more complete)
        score += (candidate.length / 50.0).coerceAtMost(3.0)

        Pair(candidate, score)
    }

    // Get the highest scoring candidate
    val bestCandidate = scoredCandidates.maxByOrNull { it.second }?.first

    // Log for debugging
    android.util.Log.d("AddressExtraction", "Candidates found: ${candidates.size}")
    scoredCandidates.forEach { (candidate, score) ->
        android.util.Log.d("AddressExtraction", "  ${candidate.text} (score: $score)")
    }
    android.util.Log.d("AddressExtraction", "Selected: ${bestCandidate?.text}")

    // VALIDATE: Check if the extracted address matches any stop on the route
    val extractedAddress = bestCandidate?.text ?: return null

    // Improved fuzzy matching with the route stops
    val matchingStop = findBestAddressMatch(extractedAddress, routeStops)

    if (matchingStop != null) {
        android.util.Log.d("AddressExtraction", "✓ VALIDATED: Address found on route (matched: ${matchingStop.addressLabel})")
        return extractedAddress
    } else {
        android.util.Log.w("AddressExtraction", "✗ REJECTED: Address '$extractedAddress' NOT on route (${routeStops.size} stops checked)")
        return null  // Reject addresses not on the route
    }
}

/**
 * Find the best matching stop for an extracted address using fuzzy matching.
 * Handles street name abbreviations and variations.
 */
private fun findBestAddressMatch(
    extractedAddress: String,
    routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>
): com.isaacshub.app.routehelper.data.RoutedStopEntity? {
    val normalizedExtracted = normalizeAddress(extractedAddress)

    // First try exact match
    routeStops.find { stop ->
        normalizeAddress(stop.addressLabel) == normalizedExtracted
    }?.let { return it }

    // Try matching just the street number (address numbers must match)
    val extractedNumber = extractAddressNumber(normalizedExtracted)
    if (extractedNumber == null) {
        // No number found, try substring matching
        return routeStops.find { stop ->
            val normalizedStop = normalizeAddress(stop.addressLabel)
            normalizedStop.contains(normalizedExtracted) ||
            normalizedExtracted.contains(normalizedStop)
        }
    }

    // Find stops with matching address number
    val candidatesWithMatchingNumber = routeStops.filter { stop ->
        extractAddressNumber(normalizeAddress(stop.addressLabel)) == extractedNumber
    }

    if (candidatesWithMatchingNumber.isEmpty()) {
        return null  // No stops with matching number
    }

    if (candidatesWithMatchingNumber.size == 1) {
        return candidatesWithMatchingNumber.first()  // Only one match, return it
    }

    // Multiple stops with same number - use fuzzy matching on street name
    val extractedStreet = extractStreetName(normalizedExtracted)
    return candidatesWithMatchingNumber.maxByOrNull { stop ->
        val stopStreet = extractStreetName(normalizeAddress(stop.addressLabel))
        calculateStreetNameSimilarity(extractedStreet, stopStreet)
    }
}

/**
 * Normalize an address for comparison:
 * - Lowercase
 * - Remove extra whitespace
 * - Expand common abbreviations
 */
private fun normalizeAddress(address: String): String {
    return address
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")  // Collapse multiple spaces
        .replace("saint", "st")
        .replace("avenue", "ave")
        .replace("street", "st")
        .replace("drive", "dr")
        .replace("road", "rd")
        .replace("boulevard", "blvd")
        .replace("lane", "ln")
        .replace("court", "ct")
        .replace("place", "pl")
        .replace("circle", "cir")
        .replace("north", "n")
        .replace("south", "s")
        .replace("east", "e")
        .replace("west", "w")
}

/**
 * Extract the address number from a normalized address.
 * E.g., "1006 jackson st" -> 1006
 */
private fun extractAddressNumber(normalizedAddress: String): Int? {
    return normalizedAddress.split(" ").firstOrNull()?.toIntOrNull()
}

/**
 * Extract the street name from a normalized address (everything after the number).
 * E.g., "1006 jackson st" -> "jackson st"
 */
private fun extractStreetName(normalizedAddress: String): String {
    val parts = normalizedAddress.split(" ", limit = 2)
    return if (parts.size > 1) parts[1] else normalizedAddress
}

/**
 * Calculate similarity between two street names.
 * Returns a score from 0.0 (no match) to 1.0 (perfect match).
 */
private fun calculateStreetNameSimilarity(street1: String, street2: String): Double {
    // Exact match
    if (street1 == street2) return 1.0

    // One contains the other
    if (street1.contains(street2) || street2.contains(street1)) return 0.8

    // Check if key words match (ignoring common suffixes)
    val words1 = street1.split(" ").filter { it.length > 2 }  // Filter out short words like "st"
    val words2 = street2.split(" ").filter { it.length > 2 }

    val matchingWords = words1.count { w1 ->
        words2.any { w2 -> w1 == w2 || w1.startsWith(w2) || w2.startsWith(w1) }
    }

    return if (words1.isEmpty() || words2.isEmpty()) {
        0.0
    } else {
        matchingWords.toDouble() / maxOf(words1.size, words2.size)
    }
}

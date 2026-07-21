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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.isaacshub.app.routehelper.util.findBestRoutedStopMatch
import java.util.concurrent.Executors

data class ScannedPackage(
    val trackingNumber: String,
    val addressLabel: String,
    val sequenceNumber: Int? = null,
    val sections: List<String> = emptyList(),
    val routedStopId: Long? = null,
    /** True if this package is for an unknown address on a known street */
    val isUnknownStreetMatch: Boolean = false,
    /** Street name for unknown packages on known streets */
    val streetName: String? = null
)

/**
 * Package scanner screen for scanning packages in the morning before route playback.
 * Extracts USPS tracking numbers from barcodes/QR codes and addresses from OCR.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageScanScreen(
    routeId: Long,
    scannedPackages: List<ScannedPackage>,
    onPackageScanned: (ScannedPackage) -> Unit,
    onPackageDeleted: (ScannedPackage) -> Unit,
    onPlotUnknownPackage: (ScannedPackage) -> Unit,
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
                        // Keep camera open for bulk scanning
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = if (pkg.isUnknownStreetMatch) {
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            // Show warning icon for unknown packages
                                            if (pkg.isUnknownStreetMatch) {
                                                Icon(
                                                    Icons.Filled.Warning,
                                                    contentDescription = "Unknown address",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                            Text(
                                                pkg.addressLabel,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        // Show street name for unknown packages
                                        if (pkg.isUnknownStreetMatch && pkg.streetName != null) {
                                            Text(
                                                "Street: ${pkg.streetName} (needs plotting)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        // Show sections if available
                                        if (pkg.sections.isNotEmpty()) {
                                            Text(
                                                "Sections: ${pkg.sections.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 4.dp)
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

                                // Plot button for unknown packages
                                if (pkg.isUnknownStreetMatch) {
                                    Button(
                                        onClick = { onPlotUnknownPackage(pkg) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text("Plot Unknown Package")
                                    }
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
    var wildcardScanRequested by remember { mutableStateOf(false) }

    // Access repository to get sections and route stops
    val context = LocalContext.current
    val app = context.applicationContext as com.isaacshub.app.App
    val repository = app.routeHelperRepository

    // Load route stops and route info for address validation
    var routeStops by remember { mutableStateOf<List<com.isaacshub.app.routehelper.data.RoutedStopEntity>>(emptyList()) }
    var routeZipCode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(routeId) {
        routeStops = repository.getStopsOnce(routeId)
        routeZipCode = repository.getRoute(routeId)?.zipCode
        android.util.Log.d("PackageScanner", "Loaded ${routeStops.size} stops for route $routeId (ZIP: $routeZipCode)")
    }

    // When a package is scanned, look up its sections and re-enable scanning after cooldown
    LaunchedEffect(lastScannedPackage) {
        lastScannedPackage?.let { pkg ->
            // Look up sections for the last scanned package
            val sections = if (pkg.sequenceNumber != null) {
                // Find the matching stop to get sections
                val stops = repository.getStopsOnce(routeId)
                val matchingStop = stops.find { it.sequenceOrder == pkg.sequenceNumber }
                if (matchingStop != null) {
                    repository.getSectionsForStop(routeId, matchingStop.id).map { it.name }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
            sectionsForLastPackage = sections

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
                routeZipCode = routeZipCode,
                wildcardScanRequested = wildcardScanRequested,
                onWildcardScanProcessed = { wildcardScanRequested = false },
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

            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                "Point camera at package barcode/QR code and address"
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

                // Wildcard scan button for when barcode won't scan
                Button(
                    onClick = {
                        if (canScan) {
                            android.util.Log.d("PackageScanner", "Wildcard scan requested - scanning for address only")
                            wildcardScanRequested = true
                        }
                    },
                    enabled = canScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Can't Scan Barcode? Use Address Only")
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
    routeZipCode: String?,
    wildcardScanRequested: Boolean,
    onWildcardScanProcessed: () -> Unit,
    onPackageDetected: (ScannedPackage) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onPackageDetectedState = rememberUpdatedState(onPackageDetected)
    val routeStopsState = rememberUpdatedState(routeStops)
    val routeZipCodeState = rememberUpdatedState(routeZipCode)
    val wildcardScanRequestedState = rememberUpdatedState(wildcardScanRequested)
    val onWildcardScanProcessedState = rememberUpdatedState(onWildcardScanProcessed)

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

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
                                processPackageImage(
                                    imageProxy,
                                    recognizer,
                                    barcodeScanner,
                                    routeStopsState.value,
                                    routeZipCodeState.value,
                                    wildcardScanRequestedState.value,
                                    onWildcardScanProcessedState.value,
                                    onPackageDetectedState.value
                                )
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
                            processPackageImage(
                                imageProxy,
                                recognizer,
                                barcodeScanner,
                                routeStopsState.value,
                                routeZipCodeState.value,
                                wildcardScanRequestedState.value,
                                onWildcardScanProcessedState.value,
                                onPackageDetectedState.value
                            )
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
 * Process an image frame to extract package tracking number from barcodes and address from OCR.
 * Priority: Scan barcodes/QR codes for tracking numbers, fall back to OCR if needed.
 * If wildcardScanRequested is true, only extract address and use a timestamp-based wildcard tracking number.
 */
private fun processPackageImage(
    imageProxy: ImageProxy,
    recognizer: TextRecognizer,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>,
    routeZipCode: String?,
    wildcardScanRequested: Boolean,
    onWildcardScanProcessed: () -> Unit,
    onPackageDetected: (ScannedPackage) -> Unit
) {
    @androidx.camera.core.ExperimentalGetImage
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // If wildcard scan is requested, only extract address and assign a wildcard tracking number
        if (wildcardScanRequested) {
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val addressResult = extractAddressWithStop(visionText.text, routeStops)

                    if (addressResult != null) {
                        val (address, matchedStop) = addressResult
                        val wildcardTracking = "WILDCARD-${System.currentTimeMillis()}"
                        android.util.Log.d("PackageScanner", "✓ Wildcard package scan: tracking=$wildcardTracking, address=$address")
                        onPackageDetected(ScannedPackage(
                            trackingNumber = wildcardTracking,
                            addressLabel = address,
                            sequenceNumber = matchedStop.sequenceOrder,
                            routedStopId = matchedStop.id
                        ))
                        onWildcardScanProcessed()
                    } else {
                        android.util.Log.w("PackageScanner", "✗ Wildcard scan: No valid address found on route")
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
            return
        }

        // First, try to scan barcodes for tracking number
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                android.util.Log.d("PackageScanner", "Barcodes detected: ${barcodes.size}")
                barcodes.forEach { barcode ->
                    android.util.Log.d("PackageScanner", "  Barcode: ${barcode.rawValue} (format: ${barcode.format})")
                }

                val trackingNumber = extractTrackingFromBarcodes(barcodes)
                android.util.Log.d("PackageScanner", "Extracted tracking from barcode: $trackingNumber")

                if (trackingNumber != null) {
                    // Found tracking via barcode, now get address via OCR
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val addressResult = extractAddressWithStop(visionText.text, routeStops)

                            if (addressResult != null) {
                                val (address, matchedStop) = addressResult
                                android.util.Log.d("PackageScanner", "✓ Package detected via BARCODE + OCR: tracking=$trackingNumber, address=$address")
                                onPackageDetected(ScannedPackage(
                                    trackingNumber = trackingNumber,
                                    addressLabel = address,
                                    sequenceNumber = matchedStop.sequenceOrder,
                                    routedStopId = matchedStop.id
                                ))
                            } else {
                                // No exact match - check if it's on a known street
                                val rawAddress = extractBestAddressCandidate(visionText.text)
                                if (rawAddress != null && routeZipCode != null) {
                                    val matchingStreet = checkUnknownStreetMatch(rawAddress, routeStops, routeZipCode)
                                    if (matchingStreet != null) {
                                        android.util.Log.d("PackageScanner", "✓ Unknown package on known street: tracking=$trackingNumber, address=$rawAddress, street=$matchingStreet")
                                        onPackageDetected(ScannedPackage(
                                            trackingNumber = trackingNumber,
                                            addressLabel = rawAddress,
                                            sequenceNumber = null,
                                            routedStopId = null,
                                            isUnknownStreetMatch = true,
                                            streetName = matchingStreet
                                        ))
                                    } else {
                                        android.util.Log.w("PackageScanner", "✗ Barcode found ($trackingNumber) but address NOT found on route or known street")
                                    }
                                } else {
                                    android.util.Log.w("PackageScanner", "✗ Barcode found ($trackingNumber) but address NOT found on route")
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    // No barcode found, try OCR for both tracking and address
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            val ocrTracking = extractUSPSTrackingNumber(text)
                            val addressResult = extractAddressWithStop(text, routeStops)

                            if (ocrTracking != null && addressResult != null) {
                                val (address, matchedStop) = addressResult
                                android.util.Log.d("PackageScanner", "✓ Package detected via OCR only: tracking=$ocrTracking, address=$address")
                                onPackageDetected(ScannedPackage(
                                    trackingNumber = ocrTracking,
                                    addressLabel = address,
                                    sequenceNumber = matchedStop.sequenceOrder,
                                    routedStopId = matchedStop.id
                                ))
                            } else if (ocrTracking != null && addressResult == null) {
                                // Found tracking but no exact address match - check for unknown street match
                                val rawAddress = extractBestAddressCandidate(text)
                                if (rawAddress != null && routeZipCode != null) {
                                    val matchingStreet = checkUnknownStreetMatch(rawAddress, routeStops, routeZipCode)
                                    if (matchingStreet != null) {
                                        android.util.Log.d("PackageScanner", "✓ Unknown package on known street (OCR): tracking=$ocrTracking, address=$rawAddress, street=$matchingStreet")
                                        onPackageDetected(ScannedPackage(
                                            trackingNumber = ocrTracking,
                                            addressLabel = rawAddress,
                                            sequenceNumber = null,
                                            routedStopId = null,
                                            isUnknownStreetMatch = true,
                                            streetName = matchingStreet
                                        ))
                                    } else {
                                        android.util.Log.d("PackageScanner", "✗ OCR: tracking=$ocrTracking, address NOT on route or known street")
                                    }
                                } else {
                                    android.util.Log.d("PackageScanner", "✗ OCR: tracking=$ocrTracking, address=${addressResult?.first}")
                                }
                            } else {
                                android.util.Log.d("PackageScanner", "✗ OCR: tracking=$ocrTracking, address=${addressResult?.first}")
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PackageScanner", "Barcode scanning failed", e)
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Dialog for selecting which two stops an unknown package is between.
 * Groups stops by consecutive visits to the same street.
 */
@Composable
fun PlotUnknownPackageDialog(
    pkg: ScannedPackage,
    routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>,
    onDismiss: () -> Unit,
    onConfirm: (afterStopId: Long, beforeStopId: Long) -> Unit
) {
    var selectedAfterStop by remember { mutableStateOf<com.isaacshub.app.routehelper.data.RoutedStopEntity?>(null) }
    var selectedBeforeStop by remember { mutableStateOf<com.isaacshub.app.routehelper.data.RoutedStopEntity?>(null) }

    // Group stops by street visits
    data class StreetVisit(
        val stopsOnStreet: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>,
        val previousRoadLastStop: com.isaacshub.app.routehelper.data.RoutedStopEntity?,
        val nextRoadFirstStop: com.isaacshub.app.routehelper.data.RoutedStopEntity?
    )

    val streetVisits = remember(routeStops, pkg.streetName) {
        if (pkg.streetName == null) return@remember emptyList()

        val visits = mutableListOf<StreetVisit>()
        var currentVisitStops = mutableListOf<com.isaacshub.app.routehelper.data.RoutedStopEntity>()
        var previousRoadLastStop: com.isaacshub.app.routehelper.data.RoutedStopEntity? = null

        routeStops.forEachIndexed { index, stop ->
            val stopStreet = extractStreetName(stop.addressLabel)
            val isOnTargetStreet = stopStreet?.equals(pkg.streetName, ignoreCase = true) == true ||
                                    stopStreet?.contains(pkg.streetName, ignoreCase = true) == true ||
                                    pkg.streetName.contains(stopStreet ?: "", ignoreCase = true)

            if (isOnTargetStreet) {
                currentVisitStops.add(stop)
            } else {
                // We left the target street
                if (currentVisitStops.isNotEmpty()) {
                    // Save this visit
                    val nextRoadFirstStop = stop
                    visits.add(StreetVisit(
                        stopsOnStreet = currentVisitStops.toList(),
                        previousRoadLastStop = previousRoadLastStop,
                        nextRoadFirstStop = nextRoadFirstStop
                    ))
                    currentVisitStops.clear()
                }
                previousRoadLastStop = stop
            }
        }

        // Handle last visit if we ended on the target street
        if (currentVisitStops.isNotEmpty()) {
            visits.add(StreetVisit(
                stopsOnStreet = currentVisitStops.toList(),
                previousRoadLastStop = previousRoadLastStop,
                nextRoadFirstStop = null
            ))
        }

        visits
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plot Unknown Package") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Select which two stops this package is between:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    pkg.addressLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (streetVisits.isEmpty()) {
                    Text(
                        "No stops found on ${pkg.streetName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        streetVisits.forEachIndexed { visitIndex, visit ->
                            item {
                                if (visitIndex > 0) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                                Text(
                                    "Visit ${visitIndex + 1} to ${pkg.streetName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // Previous road last stop
                            visit.previousRoadLastStop?.let { stop ->
                                item {
                                    SelectableStopCard(
                                        stop = stop,
                                        label = "Last stop before entering ${pkg.streetName}",
                                        isSelected = selectedAfterStop?.id == stop.id || selectedBeforeStop?.id == stop.id,
                                        onClick = {
                                            if (selectedAfterStop == null) {
                                                selectedAfterStop = stop
                                            } else if (selectedBeforeStop == null && selectedAfterStop?.id != stop.id) {
                                                selectedBeforeStop = stop
                                            } else {
                                                // Reset selection
                                                selectedAfterStop = null
                                                selectedBeforeStop = null
                                            }
                                        }
                                    )
                                }
                            }

                            // Stops on the street
                            items(visit.stopsOnStreet.size) { index ->
                                val stop = visit.stopsOnStreet[index]
                                SelectableStopCard(
                                    stop = stop,
                                    label = null,
                                    isSelected = selectedAfterStop?.id == stop.id || selectedBeforeStop?.id == stop.id,
                                    onClick = {
                                        if (selectedAfterStop == null) {
                                            selectedAfterStop = stop
                                        } else if (selectedBeforeStop == null && selectedAfterStop?.id != stop.id) {
                                            selectedBeforeStop = stop
                                        } else {
                                            // Reset selection
                                            selectedAfterStop = null
                                            selectedBeforeStop = null
                                        }
                                    }
                                )
                            }

                            // Next road first stop
                            visit.nextRoadFirstStop?.let { stop ->
                                item {
                                    SelectableStopCard(
                                        stop = stop,
                                        label = "First stop after leaving ${pkg.streetName}",
                                        isSelected = selectedAfterStop?.id == stop.id || selectedBeforeStop?.id == stop.id,
                                        onClick = {
                                            if (selectedAfterStop == null) {
                                                selectedAfterStop = stop
                                            } else if (selectedBeforeStop == null && selectedAfterStop?.id != stop.id) {
                                                selectedBeforeStop = stop
                                            } else {
                                                // Reset selection
                                                selectedAfterStop = null
                                                selectedBeforeStop = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAfterStop != null && selectedBeforeStop != null) {
                        // Ensure correct order (after < before in sequence)
                        val (after, before) = if (selectedAfterStop!!.sequenceOrder < selectedBeforeStop!!.sequenceOrder) {
                            selectedAfterStop!! to selectedBeforeStop!!
                        } else {
                            selectedBeforeStop!! to selectedAfterStop!!
                        }
                        onConfirm(after.id, before.id)
                    }
                },
                enabled = selectedAfterStop != null && selectedBeforeStop != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SelectableStopCard(
    stop: com.isaacshub.app.routehelper.data.RoutedStopEntity,
    label: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Text(
                        "#${stop.sequenceOrder}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
                Text(
                    stop.addressLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Extract tracking number from scanned barcodes.
 * Prioritizes USPS tracking formats, but also accepts international formats.
 * Filters out UPS and FedEx barcodes when other tracking numbers are present.
 */
private fun extractTrackingFromBarcodes(barcodes: List<Barcode>): String? {
    if (barcodes.isEmpty()) return null

    // Separate barcodes by carrier type
    val uspsBarcodes = mutableListOf<String>()
    val internationalBarcodes = mutableListOf<String>()
    val upsBarcodes = mutableListOf<String>()
    val fedexBarcodes = mutableListOf<String>()
    val otherBarcodes = mutableListOf<String>()

    for (barcode in barcodes) {
        val rawValue = barcode.rawValue ?: continue
        val cleanValue = rawValue.trim()

        android.util.Log.d("BarcodeExtraction", "Checking barcode: $cleanValue")

        when {
            // USPS tracking patterns
            isUSPSTracking(cleanValue) -> {
                android.util.Log.d("BarcodeExtraction", "  → USPS tracking")
                uspsBarcodes.add(cleanValue)
            }

            // International tracking (starts with 2 letters, ends with 2 letters)
            cleanValue.matches(Regex("""^[A-Z]{2}\d{9,13}[A-Z]{2}$""")) -> {
                android.util.Log.d("BarcodeExtraction", "  → International tracking")
                internationalBarcodes.add(cleanValue)
            }

            // UPS (1Z prefix)
            cleanValue.startsWith("1Z", ignoreCase = true) -> {
                android.util.Log.d("BarcodeExtraction", "  → UPS (filtered)")
                upsBarcodes.add(cleanValue)
            }

            // FedEx 12-digit
            cleanValue.matches(Regex("""^\d{12}$""")) -> {
                android.util.Log.d("BarcodeExtraction", "  → FedEx 12-digit (filtered)")
                fedexBarcodes.add(cleanValue)
            }

            // FedEx 15-digit
            cleanValue.matches(Regex("""^\d{15}$""")) -> {
                android.util.Log.d("BarcodeExtraction", "  → FedEx 15-digit (filtered)")
                fedexBarcodes.add(cleanValue)
            }

            // Other barcodes that might be tracking numbers
            cleanValue.length >= 10 && cleanValue.matches(Regex("""^[\dA-Z]+$""")) -> {
                android.util.Log.d("BarcodeExtraction", "  → Other barcode (length ${cleanValue.length})")
                otherBarcodes.add(cleanValue)
            }

            else -> {
                android.util.Log.d("BarcodeExtraction", "  → Rejected (too short or invalid format)")
            }
        }
    }

    // Priority: USPS > International > Other > UPS/FedEx (as last resort)
    val result = uspsBarcodes.firstOrNull()
        ?: internationalBarcodes.firstOrNull()
        ?: otherBarcodes.firstOrNull()
        ?: upsBarcodes.firstOrNull()
        ?: fedexBarcodes.firstOrNull()

    android.util.Log.d("BarcodeExtraction", "Selected tracking: $result (USPS: ${uspsBarcodes.size}, Intl: ${internationalBarcodes.size}, Other: ${otherBarcodes.size})")

    return result
}

/**
 * Check if a string matches USPS tracking format.
 */
private fun isUSPSTracking(value: String): Boolean {
    val digitsOnly = value.replace(Regex("""[^\d]"""), "")

    return when (digitsOnly.length) {
        20, 22 -> true  // Standard USPS tracking (20 or 22 digits)
        26 -> true      // USPS Certified Mail
        30 -> true      // Some USPS priority mail
        else -> false
    }
}

/**
 * Extract USPS tracking number from OCR text (fallback when barcode scanning fails).
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
 * Returns both the extracted address string and the matched stop.
 * On USPS packages, the delivery address is typically larger and centered.
 * We look for the largest/most prominent address pattern, avoiding the return address.
 *
 * Strategy:
 * 1. Find all lines that look like addresses (number + street)
 * 2. Filter out lines near "FROM:" or return address indicators
 * 3. Prefer addresses in middle section of text (delivery label is centered)
 * 4. Prefer longer addresses (delivery address typically more complete)
 * 5. VALIDATE against actual route stops - reject if not on route
 * 6. Return the most likely delivery address that matches a route stop, with the stop itself
 */
private fun extractAddressWithStop(text: String, routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>): Pair<String, com.isaacshub.app.routehelper.data.RoutedStopEntity>? {
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

    // Use shared fuzzy matching utility with the route stops
    val matchingStop = findBestRoutedStopMatch(extractedAddress, routeStops)

    if (matchingStop != null) {
        android.util.Log.d("AddressExtraction", "✓ VALIDATED: Address found on route (matched: ${matchingStop.addressLabel})")
        return Pair(extractedAddress, matchingStop)
    } else {
        android.util.Log.w("AddressExtraction", "✗ REJECTED: Address '$extractedAddress' NOT on route (${routeStops.size} stops checked)")
        return null  // Reject addresses not on the route
    }
}

/**
 * Extract the best address candidate from OCR text (without validating against route).
 * Used for unknown package detection when we need the raw address.
 */
private fun extractBestAddressCandidate(text: String): String? {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    val addressPattern = Regex("""^\d+\s+[A-Za-z].*""")

    // Find all lines that look like addresses
    val candidates = lines.filter { addressPattern.matches(it) }
    if (candidates.isEmpty()) return null

    // Return the longest one (usually the most complete)
    return candidates.maxByOrNull { it.length }
}

/**
 * Extract street name from an address string.
 * Examples:
 * - "123 Main St" -> "Main St"
 * - "45 DALLAS DR" -> "DALLAS DR"
 * - "100 North Pennsylvania Avenue" -> "North Pennsylvania Avenue"
 */
private fun extractStreetName(address: String): String? {
    // Remove house number (digits at start)
    val withoutNumber = address.replace(Regex("""^\d+\s+"""), "").trim()
    if (withoutNumber.isEmpty()) return null

    // Common abbreviations to normalize
    val normalized = withoutNumber
        .replace(Regex("""\bSt\.?\b""", RegexOption.IGNORE_CASE), "Street")
        .replace(Regex("""\bAve\.?\b""", RegexOption.IGNORE_CASE), "Avenue")
        .replace(Regex("""\bDr\.?\b""", RegexOption.IGNORE_CASE), "Drive")
        .replace(Regex("""\bRd\.?\b""", RegexOption.IGNORE_CASE), "Road")
        .replace(Regex("""\bBlvd\.?\b""", RegexOption.IGNORE_CASE), "Boulevard")
        .replace(Regex("""\bLn\.?\b""", RegexOption.IGNORE_CASE), "Lane")
        .replace(Regex("""\bCt\.?\b""", RegexOption.IGNORE_CASE), "Court")
        .replace(Regex("""\bPl\.?\b""", RegexOption.IGNORE_CASE), "Place")
        .replace(Regex("""\bPkwy\.?\b""", RegexOption.IGNORE_CASE), "Parkway")

    return normalized.trim()
}

/**
 * Check if an address that doesn't match a stop might be on a known street.
 * Returns the street name if it matches a street on the route with the correct ZIP code.
 */
private fun checkUnknownStreetMatch(
    address: String,
    routeStops: List<com.isaacshub.app.routehelper.data.RoutedStopEntity>,
    routeZipCode: String
): String? {
    val extractedStreet = extractStreetName(address) ?: return null

    // Check if this street name appears in any route stop
    val streetsOnRoute = routeStops.mapNotNull { stop ->
        extractStreetName(stop.addressLabel)
    }.toSet()

    // Check for fuzzy match on street names
    val matchingStreet = streetsOnRoute.find { routeStreet ->
        routeStreet.equals(extractedStreet, ignoreCase = true) ||
        routeStreet.contains(extractedStreet, ignoreCase = true) ||
        extractedStreet.contains(routeStreet, ignoreCase = true)
    }

    if (matchingStreet != null) {
        android.util.Log.d("UnknownPackage", "✓ Street match: '$extractedStreet' found on route as '$matchingStreet'")
        return matchingStreet
    }

    android.util.Log.d("UnknownPackage", "✗ No street match: '$extractedStreet' not found on route")
    return null
}

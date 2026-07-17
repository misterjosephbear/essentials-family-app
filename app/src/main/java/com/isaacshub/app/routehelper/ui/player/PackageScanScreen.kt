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
    val addressLabel: String
)

/**
 * Package scanner screen for scanning packages in the morning before route playback.
 * Extracts USPS tracking numbers and addresses from package labels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageScanScreen(
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
                                    Text(
                                        pkg.addressLabel,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
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
    onPackageScanned: (ScannedPackage) -> Unit,
    onCancel: () -> Unit
) {
    var useFrontCamera by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(true) }

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
                onPackageDetected = { pkg ->
                    if (scanning) {
                        scanning = false
                        onPackageScanned(pkg)
                    }
                }
            )

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Point camera at package label",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Make sure tracking number and address are visible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (!scanning) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Processing...",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageCameraPreview(
    useFrontCamera: Boolean,
    onPackageDetected: (ScannedPackage) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onPackageDetectedState = rememberUpdatedState(onPackageDetected)

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
                                processPackageImage(imageProxy, recognizer, onPackageDetectedState.value)
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
                            processPackageImage(imageProxy, recognizer, onPackageDetectedState.value)
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
                val address = extractAddress(text)

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
 * Extract address from OCR text.
 * Looks for patterns like: number + street name + optional unit
 */
private fun extractAddress(text: String): String? {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    // Look for a line that starts with a number (street address)
    val addressPattern = Regex("""^\d+\s+[A-Za-z].*""")

    for (line in lines) {
        if (addressPattern.matches(line)) {
            return line
        }
    }

    return null
}

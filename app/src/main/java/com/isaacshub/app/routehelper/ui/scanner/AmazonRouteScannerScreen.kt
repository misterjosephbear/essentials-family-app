package com.isaacshub.app.routehelper.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Amazon Route Scanner Screen - Scans addresses from a list (like a USPS scanner screen)
 * and builds a route in the exact order scanned.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmazonRouteScannerScreen(
    routeId: Long,
    onComplete: () -> Unit
) {
    val viewModel: AmazonRouteScannerViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(routeId) {
        viewModel.start(routeId)
    }

    // Show validation messages as snackbar
    LaunchedEffect(uiState.validationMessage) {
        uiState.validationMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearValidationMessage()
        }
    }

    // Show errors as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Image picker for OCR testing (developer mode)
    val context = LocalContext.current
    var showImagePicker by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.testOcrWithImage(context, selectedUri)
        }
    }

    LaunchedEffect(showImagePicker) {
        if (showImagePicker) {
            imagePickerLauncher.launch("image/*")
            showImagePicker = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scan Addresses (${uiState.scannedAddresses.size})") },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // OCR test button (always visible for developer testing)
                    IconButton(onClick = { showImagePicker = true }) {
                        Icon(Icons.Filled.Image, contentDescription = "Test OCR with Image")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.scannedAddresses.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.finishScanning(onComplete) }
                ) {
                    if (uiState.isCreatingStops) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = "Finish & Create Route")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Camera preview section (top 40% of screen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                ) {
                    AddressCameraScanner(
                        candidateAddresses = uiState.candidateAddresses,
                        currentAddressCount = uiState.scannedAddresses.size,
                        onAddressScanned = { address, scannerSeq, quantity, routeId ->
                            viewModel.onAddressScanned(address, scannerSeq, quantity, routeId)
                        }
                    )

                    // Scanning status overlay
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Point camera at address list",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Scroll to scan multiple addresses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.lastScannedAddress != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Last: ${uiState.lastScannedAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Scanned addresses list (bottom 60% of screen)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Text(
                            "Scanned Addresses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        if (uiState.scannedAddresses.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No addresses scanned yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.scannedAddresses) { address ->
                                    AddressListItem(
                                        address = address,
                                        onDelete = { viewModel.removeAddress(address) }
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

@Composable
private fun AddressListItem(
    address: ScannedAddress,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isValid) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status icon
                Icon(
                    imageVector = if (address.isValid) Icons.Filled.Check else Icons.Filled.Warning,
                    contentDescription = if (address.isValid) "Valid" else "Unmatched",
                    tint = if (address.isValid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${address.sequenceNumber}. ${address.addressLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (!address.isValid) {
                        Text(
                            "Not matched to ZIP code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    // Show scanner sequence number if detected
                    if (address.scannerSequenceNumber != null) {
                        val seqMatches = address.scannerSequenceNumber == address.sequenceNumber
                        Text(
                            "Scanner seq: #${address.scannerSequenceNumber}${if (!seqMatches) " (mismatch!)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (seqMatches) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    // Show expected package count if available
                    if (address.expectedPackageCount != null) {
                        Text(
                            "Expected packages: ${address.expectedPackageCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddressCameraScanner(
    candidateAddresses: List<com.isaacshub.app.routehelper.data.CandidateAddressEntity>,
    currentAddressCount: Int,  // Number of addresses already scanned
    onAddressScanned: (String, Int?, Int?, String?) -> Unit  // address, scannerSequenceNumber, quantity, routeId
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        var canScan by remember { mutableStateOf(true) }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                if (canScan) {
                                    // Calculate expected next sequence (1-based)
                                    val expectedNextSeq = currentAddressCount + 1
                                    processAddressImage(imageProxy, recognizer, expectedNextSeq) { extracted ->
                                        if (extracted != null) {
                                            canScan = false
                                            onAddressScanned(
                                                extracted.address,
                                                extracted.sequenceNumber,
                                                extracted.quantity,
                                                extracted.routeId
                                            )

                                            // Re-enable scanning after 2-second cooldown
                                            MainScope().launch {
                                                delay(2000)
                                                canScan = true
                                            }
                                        }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CameraScanner", "Failed to bind camera", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission required")
        }
    }
}

/**
 * Process a camera frame to extract address text.
 */
private fun processAddressImage(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    expectedNextSequence: Int,
    onAddressFound: (ExtractedAddress?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Try direction sheet parser first (more structured format)
                var extractedAddress = extractFromDirectionSheet(visionText.text, expectedNextSequence)

                // Fall back to regular address parser if direction sheet parse failed
                if (extractedAddress == null) {
                    extractedAddress = extractAddress(visionText.text, expectedNextSequence)
                }

                onAddressFound(extractedAddress)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AddressScanner", "Text recognition failed", e)
                onAddressFound(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
        onAddressFound(null)
    }
}

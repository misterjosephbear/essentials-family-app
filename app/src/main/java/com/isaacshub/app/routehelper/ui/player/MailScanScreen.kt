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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.isaacshub.app.routehelper.domain.GeoPoint

/**
 * Full-screen mail-piece scanner: points the front camera at a letter, OCRs each frame looking for an
 * address-shaped block, and hands the resolved (geocoded) result back via [onResolved]. If the frame
 * carries more than one address-shaped block, the driver is asked which one is correct before it's
 * geocoded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScanScreen(currentLocation: GeoPoint?, onResolved: (ResolvedMailStop) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MailScanViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val resolved by viewModel.resolved.collectAsState()

    LaunchedEffect(currentLocation) {
        currentLocation?.let { viewModel.setFallbackLocation(it) }
    }
    LaunchedEffect(resolved) { resolved?.let(onResolved) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scan mail piece") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel scan")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission) {
                MailCameraPreview(onTextRecognized = viewModel::onTextRecognized)
            } else {
                Text(
                    "Camera access is needed to scan a mail piece.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            }

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Hold the address side of the mail piece up to the front camera.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (state.resolving) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Looking up that address...")
                }
            }

            state.error?.let { error ->
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::dismissError) { Text("Try again") }
                }
            }
        }
    }

    if (state.candidates.size > 1) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Which address is correct?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.candidates.forEach { candidate ->
                        TextButton(onClick = { viewModel.choose(candidate) }) {
                            Text(candidate.addressText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("Rescan") }
            }
        )
    }
}

@Composable
private fun MailCameraPreview(onTextRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onTextRecognizedState = rememberUpdatedState(onTextRecognized)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    processImageProxy(recognizer, imageProxy, onTextRecognizedState.value)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                    // Camera bind can fail if the lifecycle is already destroyed by the time this runs.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun processImageProxy(recognizer: TextRecognizer, imageProxy: ImageProxy, onTextRecognized: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            if (visionText.text.isNotBlank()) onTextRecognized(visionText.text)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

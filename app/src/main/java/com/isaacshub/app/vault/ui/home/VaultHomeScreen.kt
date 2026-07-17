package com.isaacshub.app.vault.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timestampFormatter = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a")

private fun formatLastRun(epochMillis: Long, neverText: String): String {
    if (epochMillis == 0L) return neverText
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timestampFormatter)
}

/** Permissions needed to read photos/videos at all - required before any backup can happen. */
private fun mediaReadPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

/** Optional: without this, GPS/location EXIF data is redacted from backed-up files. Only exists API 29+. */
private fun mediaLocationPermission(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_MEDIA_LOCATION else null

private fun isGranted(context: android.content.Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    onPair: () -> Unit,
    onRestore: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: VaultHomeViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    val readPermissions = remember { mediaReadPermissions() }
    val locationPermission = remember { mediaLocationPermission() }
    val allPermissions = remember { (readPermissions + listOfNotNull(locationPermission)).toTypedArray() }

    var hasReadPermission by remember { mutableStateOf(readPermissions.all { isGranted(context, it) }) }
    var hasLocationPermission by remember {
        mutableStateOf(locationPermission?.let { isGranted(context, it) } ?: true)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasReadPermission = readPermissions.all { isGranted(context, it) }
        hasLocationPermission = locationPermission?.let { isGranted(context, it) } ?: true
    }

    LaunchedEffect(state.connection, hasReadPermission) {
        if (state.connection != null && !hasReadPermission) {
            permissionLauncher.launch(allPermissions)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Photo Vault") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val connection = state.connection
            if (connection == null) {
                Text("Not paired with a server yet.")
                Button(onClick = onPair, modifier = Modifier.fillMaxWidth()) {
                    Text("Pair a device")
                }
            } else {
                Text("Paired with ${connection.baseUrl}", style = MaterialTheme.typography.bodyLarge)

                if (!hasReadPermission) {
                    Text(
                        "Photo and video access is needed to back things up.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { permissionLauncher.launch(allPermissions) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant access")
                    }
                } else if (!hasLocationPermission) {
                    Text(
                        "Location access isn't granted - backed-up photos/videos will be missing GPS data.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { permissionLauncher.launch(allPermissions) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant location access")
                    }
                }

                Text(
                    "Photos & videos: " + formatLastRun(state.lastSyncEpochMillis, "never synced yet"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = viewModel::syncNow,
                    enabled = hasReadPermission && !state.syncingPhotos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.syncingPhotos) {
                        LoadingButtonContent(
                            label = state.photosRemaining?.let { "Backing up... ($it left)" } ?: "Backing up..."
                        )
                    } else {
                        Text("Sync now")
                    }
                }

                Text(
                    "App data: " + formatLastRun(state.lastBackupEpochMillis, "never backed up yet"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = viewModel::backupNow,
                        enabled = !state.backingUpAppData,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.backingUpAppData) {
                            LoadingButtonContent(label = "Backing up...")
                        } else {
                            Text("Back up now")
                        }
                    }
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore")
                    }
                }

                val filePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> uri?.let(viewModel::uploadPickedFile) }

                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    enabled = state.uploadingFileName == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.uploadingFileName != null) {
                        LoadingButtonContent(label = "Uploading ${state.uploadingFileName}...")
                    } else {
                        Text("Upload a file")
                    }
                }
                state.uploadError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                var remoteBaseUrlInput by remember(connection.remoteBaseUrl) {
                    mutableStateOf(connection.remoteBaseUrl.orEmpty())
                }
                Text(
                    "Remote/tunnel URL - tried when the address above can't be reached (e.g. off your home network):",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = remoteBaseUrlInput,
                    onValueChange = { remoteBaseUrlInput = it },
                    placeholder = { Text("e.g. http://isaacs-hub.playit.plus") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.setRemoteBaseUrl(remoteBaseUrlInput) },
                    enabled = remoteBaseUrlInput.trim().trimEnd('/') != connection.remoteBaseUrl.orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save remote URL")
                }

                OutlinedButton(onClick = viewModel::unpair, modifier = Modifier.fillMaxWidth()) {
                    Text("Unpair")
                }
            }
        }
    }
}

@Composable
private fun LoadingButtonContent(label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(label)
    }
}

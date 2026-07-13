package com.isaacshub.app.vault.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

private val mediaPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    onPair: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: VaultHomeViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    var hasMediaPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMediaPermission = granted
    }

    LaunchedEffect(state.connection, hasMediaPermission) {
        if (state.connection != null && !hasMediaPermission) {
            permissionLauncher.launch(mediaPermission)
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

                if (!hasMediaPermission) {
                    Text(
                        "Photo access is needed to back up new photos.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { permissionLauncher.launch(mediaPermission) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant photo access")
                    }
                }

                Text(
                    "Photos: " + formatLastRun(state.lastSyncEpochMillis, "never synced yet"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = viewModel::syncNow,
                    enabled = hasMediaPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync now")
                }

                Text(
                    "App data: " + formatLastRun(state.lastBackupEpochMillis, "never backed up yet"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = viewModel::backupNow, modifier = Modifier.fillMaxWidth()) {
                    Text("Back up now")
                }

                OutlinedButton(onClick = viewModel::unpair, modifier = Modifier.fillMaxWidth()) {
                    Text("Unpair")
                }
            }
        }
    }
}

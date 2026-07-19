package com.isaacshub.app.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.vaultPreferencesRepository)
    )
    val debugState by viewModel.debugBridgeState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("General", style = MaterialTheme.typography.titleLarge)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Discord bridge", style = MaterialTheme.typography.titleMedium)
            Text(
                "Kicks off an unattended Claude session on the server to diagnose and fix the " +
                    "discord-bridge, then posts what it found back into Discord.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = viewModel::debugDiscordBridge,
                enabled = debugState !is DebugBridgeUiState.Triggering
            ) {
                if (debugState is DebugBridgeUiState.Triggering) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Debug Discord-Bridge")
                }
            }
            when (val state = debugState) {
                is DebugBridgeUiState.Started -> Text(
                    "Started - check Discord for the result.",
                    style = MaterialTheme.typography.bodySmall
                )
                is DebugBridgeUiState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }
        }
    }
}

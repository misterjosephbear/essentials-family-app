package com.isaacshub.app.landing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onOpenSleep: () -> Unit,
    onOpenTimeTracking: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenRouteHelper: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Isaac's Hub") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolCard(
                icon = Icons.Filled.Bedtime,
                title = "Sleep Health",
                subtitle = "Track sleep sessions and sleep debt",
                onClick = onOpenSleep
            )
            ToolCard(
                icon = Icons.Filled.Schedule,
                title = "Time Tracking",
                subtitle = "Log route hours and evaluations, watch weekly overtime",
                onClick = onOpenTimeTracking
            )
            ToolCard(
                icon = Icons.Filled.CloudUpload,
                title = "Photo Vault",
                subtitle = "Back up new photos to your own server",
                onClick = onOpenVault
            )
            ToolCard(
                icon = Icons.Filled.Map,
                title = "Route Helper",
                subtitle = "Build a route live while driving it",
                onClick = onOpenRouteHelper
            )
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

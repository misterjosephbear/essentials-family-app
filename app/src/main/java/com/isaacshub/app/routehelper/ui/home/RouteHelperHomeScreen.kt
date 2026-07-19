package com.isaacshub.app.routehelper.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.routehelper.data.RouteHelperRouteEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteHelperHomeScreen(
    onOpenRoute: (Long) -> Unit,
    onOpenTestMode: () -> Unit,
    onEditRoute: (Long) -> Unit,
    onPlayRoute: (Long) -> Unit,
    onOpenAmazonScanner: (Long) -> Unit
) {
    val viewModel: RouteHelperHomeViewModel = viewModel()
    val routes by viewModel.routes.collectAsState()
    val newRouteState by viewModel.newRouteState.collectAsState()
    var showNewRouteDialog by remember { mutableStateOf(false) }
    var showAmazonRouteDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var routeToDelete by remember { mutableStateOf<RouteHelperRouteEntity?>(null) }

    LaunchedEffect(newRouteState.createdRouteId) {
        newRouteState.createdRouteId?.let { routeId ->
            showNewRouteDialog = false
            showAmazonRouteDialog = false

            // Navigate based on route type
            val route = routes.find { it.id == routeId }
            if (route?.routeType == com.isaacshub.app.routehelper.data.RouteType.AMAZON.name) {
                onOpenAmazonScanner(routeId)
            } else {
                onOpenRoute(routeId)
            }
            viewModel.consumeCreatedRoute()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route Helper") },
                actions = {
                    IconButton(onClick = onOpenTestMode) {
                        Icon(Icons.Filled.Science, contentDescription = "Testing mode")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expanded menu items
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            showNewRouteDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Regular Route")
                            Icon(Icons.Filled.Map, contentDescription = "Regular Route")
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            showAmazonRouteDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Amazon Route")
                            Icon(Icons.Filled.LocalShipping, contentDescription = "Amazon Route")
                        }
                    }
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu }
                ) {
                    Icon(
                        if (showFabMenu) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = if (showFabMenu) "Close menu" else "New route"
                    )
                }
            }
        }
    ) { padding ->

        if (routes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No routes yet. Tap + to build one from a ZIP code.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteRow(
                        route = route,
                        onClick = { onOpenRoute(route.id) },
                        onPlay = { onPlayRoute(route.id) },
                        onEdit = { onEditRoute(route.id) },
                        onDelete = { routeToDelete = route }
                    )
                }
            }
        }
    }

    if (showNewRouteDialog) {
        NewRouteDialog(
            creating = newRouteState.creating,
            error = newRouteState.error,
            onCreate = viewModel::createRoute,
            onDismiss = { if (!newRouteState.creating) showNewRouteDialog = false }
        )
    }

    if (showAmazonRouteDialog) {
        AmazonRouteDialog(
            creating = newRouteState.creating,
            error = newRouteState.error,
            onCreate = viewModel::createAmazonRoute,
            onDismiss = { if (!newRouteState.creating) showAmazonRouteDialog = false }
        )
    }

    // Delete confirmation dialog
    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Delete Route?") },
            text = {
                Text("Are you sure you want to delete route \"${route.name}\"? This will delete all stops, sections, and packages for this route. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRoute(route)
                        routeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RouteRow(
    route: RouteHelperRouteEntity,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(route.name, style = MaterialTheme.typography.titleMedium)
                val date = Instant.ofEpochMilli(route.createdAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                Text("ZIP ${route.zipCode} - built ${date.format(dateFormatter)}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.Navigation, contentDescription = "Drive this route")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View and edit stops")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete route")
                }
            }
        }
    }
}

@Composable
private fun NewRouteDialog(
    creating: Boolean,
    error: String?,
    onCreate: (name: String, zip: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New route") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Route name") },
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = zip,
                    onValueChange = { zip = it },
                    label = { Text("ZIP code") },
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth()
                )
                if (creating) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Downloading addresses for this ZIP...")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, zip) }, enabled = !creating) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AmazonRouteDialog(
    creating: Boolean,
    error: String?,
    onCreate: (zip: String) -> Unit,
    onDismiss: () -> Unit
) {
    var zip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Amazon Route") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Scan addresses from a list to create an Amazon delivery route. Name will be auto-generated.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = zip,
                    onValueChange = { zip = it },
                    label = { Text("ZIP code") },
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth()
                )
                if (creating) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Downloading addresses for this ZIP...")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(zip) }, enabled = !creating) {
                Text("Continue to Scanner")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text("Cancel")
            }
        }
    )
}

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
    onOpenRoute: (Long) -> Unit
) {
    val viewModel: RouteHelperHomeViewModel = viewModel()
    val routes by viewModel.routes.collectAsState()
    val newRouteState by viewModel.newRouteState.collectAsState()
    var showNewRouteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(newRouteState.createdRouteId) {
        newRouteState.createdRouteId?.let {
            showNewRouteDialog = false
            onOpenRoute(it)
            viewModel.consumeCreatedRoute()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Route Helper") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewRouteDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New route")
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
                    RouteRow(route = route, onClick = { onOpenRoute(route.id) }, onDelete = { viewModel.deleteRoute(route) })
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
}

@Composable
private fun RouteRow(route: RouteHelperRouteEntity, onClick: () -> Unit, onDelete: () -> Unit) {
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete route")
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

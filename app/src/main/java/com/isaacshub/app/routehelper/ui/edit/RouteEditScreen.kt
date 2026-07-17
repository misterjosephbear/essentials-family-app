package com.isaacshub.app.routehelper.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.isaacshub.app.routehelper.data.RouteSectionEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity

/** Full ordered stop list for a route, for reviewing or fixing it after the fact - reorder or remove a stop without needing to be back on location. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditScreen(routeId: Long) {
    val viewModel: RouteEditViewModel = viewModel()
    LaunchedEffect(routeId) { viewModel.start(routeId) }
    val state by viewModel.uiState.collectAsState()

    var showAddSectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.route?.name ?: "Edit route") })
        }
    ) { padding ->
        if (state.stops.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                Text("No stops routed yet.", modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sections header and list
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Route Sections", style = MaterialTheme.typography.titleLarge)
                                OutlinedButton(onClick = { showAddSectionDialog = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Section")
                                }
                            }

                            if (state.sections.isEmpty()) {
                                Text(
                                    "No sections defined. Sections help organize your route (e.g., '2B', 'Around the lake').",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                Spacer(Modifier.height(8.dp))
                                state.sections.forEach { section ->
                                    SectionRow(
                                        section = section,
                                        stops = state.stops,
                                        onDelete = { viewModel.deleteSection(section) }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                // Spacer
                item { Spacer(Modifier.height(8.dp)) }

                // Stops header
                item {
                    Text("Stops", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }

                // Stops list
                itemsIndexed(state.stops) { index, stop ->
                    StopRow(
                        position = index + 1,
                        stop = stop,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.stops.lastIndex,
                        onMoveUp = { viewModel.moveUp(stop) },
                        onMoveDown = { viewModel.moveDown(stop) },
                        onDelete = { viewModel.deleteStop(stop) }
                    )
                }
            }
        }
    }

    if (showAddSectionDialog) {
        AddSectionDialog(
            stops = state.stops,
            onDismiss = { showAddSectionDialog = false },
            onConfirm = { name, startStop, endStop ->
                viewModel.addSection(name, startStop.id, endStop.id)
                showAddSectionDialog = false
            }
        )
    }
}

@Composable
private fun StopRow(
    position: Int,
    stop: RoutedStopEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$position.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.addressLabel, style = MaterialTheme.typography.bodyLarge)
                stop.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove stop")
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: RouteSectionEntity,
    stops: List<RoutedStopEntity>,
    onDelete: () -> Unit
) {
    // Find the start and end stops
    val startStop = stops.find { it.id == section.startStopId }
    val endStop = stops.find { it.id == section.endStopId }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(section.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${startStop?.addressLabel ?: "?"} → ${endStop?.addressLabel ?: "?"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete section", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSectionDialog(
    stops: List<RoutedStopEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, startStop: RoutedStopEntity, endStop: RoutedStopEntity) -> Unit
) {
    var sectionName by remember { mutableStateOf("") }
    var selectedStartStop by remember { mutableStateOf<RoutedStopEntity?>(null) }
    var selectedEndStop by remember { mutableStateOf<RoutedStopEntity?>(null) }
    var startDropdownExpanded by remember { mutableStateOf(false) }
    var endDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Route Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("Section Name") },
                    placeholder = { Text("e.g., 2B, Around the lake") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Start stop dropdown
                ExposedDropdownMenuBox(
                    expanded = startDropdownExpanded,
                    onExpandedChange = { startDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStartStop?.addressLabel ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Address") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = startDropdownExpanded,
                        onDismissRequest = { startDropdownExpanded = false }
                    ) {
                        stops.forEachIndexed { index, stop ->
                            DropdownMenuItem(
                                text = { Text("${index + 1}. ${stop.addressLabel}") },
                                onClick = {
                                    selectedStartStop = stop
                                    startDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // End stop dropdown
                ExposedDropdownMenuBox(
                    expanded = endDropdownExpanded,
                    onExpandedChange = { endDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEndStop?.addressLabel ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Address") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = endDropdownExpanded,
                        onDismissRequest = { endDropdownExpanded = false }
                    ) {
                        stops.forEachIndexed { index, stop ->
                            DropdownMenuItem(
                                text = { Text("${index + 1}. ${stop.addressLabel}") },
                                onClick = {
                                    selectedEndStop = stop
                                    endDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sectionName.isNotBlank() && selectedStartStop != null && selectedEndStop != null) {
                        onConfirm(sectionName, selectedStartStop!!, selectedEndStop!!)
                    }
                },
                enabled = sectionName.isNotBlank() && selectedStartStop != null && selectedEndStop != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.isaacshub.app.timetracking.ui.editentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.ui.formatNumber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimeEntryScreen(
    entryId: Long?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: EditTimeEntryViewModel = viewModel(
        factory = EditTimeEntryViewModel.Factory(app.timeTrackingRepository, entryId)
    )
    val state by viewModel.uiState.collectAsState()

    var editingDate by remember { mutableStateOf(false) }
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (entryId == null) "Add time entry" else "Edit time entry") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = { editingDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Date: ${state.date.format(dateFormatter)}")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.payType == PayType.HOURLY,
                    onClick = { viewModel.setPayType(PayType.HOURLY) },
                    label = { Text("Hourly") }
                )
                FilterChip(
                    selected = state.payType == PayType.EVALUATION,
                    onClick = { viewModel.setPayType(PayType.EVALUATION) },
                    label = { Text("Evaluation") }
                )
            }

            if (state.routes.isNotEmpty()) {
                RouteSelector(
                    routes = state.routes,
                    selectedRouteId = state.routeId,
                    onSelect = viewModel::selectRoute
                )
            }

            OutlinedTextField(
                value = state.label,
                onValueChange = viewModel::setLabel,
                label = { Text("Route / job") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editingStart = true }, modifier = Modifier.weight(1f)) {
                    Text("In: ${state.startTime.format(timeFormatter)}")
                }
                OutlinedButton(onClick = { editingEnd = true }, modifier = Modifier.weight(1f)) {
                    Text("Out: ${state.endTime.format(timeFormatter)}")
                }
            }
            Text(
                "Worked: ${formatNumber(state.actualHours)}h" +
                    if (state.payType == PayType.HOURLY) " (drives your pay and overtime)" else " (drives overtime only)",
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.payType == PayType.EVALUATION) {
                val route = state.selectedRoute
                Text(
                    if (route != null) {
                        "Paid: ${formatNumber(route.evaluatedHours)} eval hrs - ${formatNumber(route.evaluatedMiles)} mi (from route)"
                    } else {
                        "Select a saved route above to pull its evaluated hours and miles"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (route != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }

            if (entryId != null) {
                OutlinedButton(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete entry")
                }
            }
        }
    }

    if (editingDate) {
        EntryDatePickerDialog(
            initial = state.date,
            onConfirm = {
                viewModel.setDate(it)
                editingDate = false
            },
            onDismiss = { editingDate = false }
        )
    }
    if (editingStart) {
        TimePickerDialog(
            initial = state.startTime,
            onConfirm = {
                viewModel.setStartTime(it)
                editingStart = false
            },
            onDismiss = { editingStart = false }
        )
    }
    if (editingEnd) {
        TimePickerDialog(
            initial = state.endTime,
            onConfirm = {
                viewModel.setEndTime(it)
                editingEnd = false
            },
            onDismiss = { editingEnd = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteSelector(
    routes: List<RouteEntity>,
    selectedRouteId: Long?,
    onSelect: (RouteEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = routes.firstOrNull { it.id == selectedRouteId }
    val selectedLabel = selected?.let { "${it.routeNumber} - ${it.cityName}" } ?: "Quick-fill from a saved route"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Saved routes") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routes.forEach { route ->
                DropdownMenuItem(
                    text = { Text("${route.routeNumber} - ${route.cityName}") },
                    onClick = {
                        onSelect(route)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedMillis = datePickerState.selectedDateMillis
                val date = if (selectedMillis != null) {
                    Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                } else {
                    initial
                }
                onConfirm(date)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = timePickerState) }
    )
}

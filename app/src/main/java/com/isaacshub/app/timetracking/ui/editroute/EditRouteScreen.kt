package com.isaacshub.app.timetracking.ui.editroute

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.timetracking.data.ScheduleType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun scheduleLabel(type: ScheduleType): String = when (type) {
    ScheduleType.NONE -> "No schedule"
    ScheduleType.WEEKLY -> "Weekly"
    ScheduleType.BIWEEKLY -> "Biweekly"
    ScheduleType.DAILY_SAT_FRI -> "Daily (Sat-Fri)"
    ScheduleType.DAILY_MON_FRI -> "Daily (Mon-Fri)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRouteScreen(
    routeId: Long?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: EditRouteViewModel = viewModel(
        factory = EditRouteViewModel.Factory(app.timeTrackingRepository, routeId)
    )
    val state by viewModel.uiState.collectAsState()

    var editingAnchor by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (routeId == null) "Add route" else "Edit route") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.routeNumber,
                onValueChange = viewModel::setRouteNumber,
                label = { Text("Route number") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.cityName,
                onValueChange = viewModel::setCityName,
                label = { Text("City name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.evaluatedHours,
                onValueChange = viewModel::setEvaluatedHours,
                label = { Text("Evaluated hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.evaluatedMiles,
                onValueChange = viewModel::setEvaluatedMiles,
                label = { Text("Evaluated miles") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            ScheduleSelector(
                selected = state.scheduleType,
                onSelect = viewModel::setScheduleType
            )

            if (state.needsAnchor) {
                OutlinedButton(onClick = { editingAnchor = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Starts on: ${state.scheduleAnchor.format(anchorFormatter)}")
                }
                Text(
                    "Sets the recurring day of week" +
                        if (state.scheduleType == ScheduleType.BIWEEKLY) " and which of the two weeks it falls on" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }

            if (routeId != null) {
                OutlinedButton(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete route")
                }
            }
        }
    }

    if (editingAnchor) {
        AnchorDatePickerDialog(
            initial = state.scheduleAnchor,
            onConfirm = {
                viewModel.setScheduleAnchor(it)
                editingAnchor = false
            },
            onDismiss = { editingAnchor = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSelector(
    selected: ScheduleType,
    onSelect: (ScheduleType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = scheduleLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("Schedule") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScheduleType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(scheduleLabel(type)) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val anchorFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnchorDatePickerDialog(
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

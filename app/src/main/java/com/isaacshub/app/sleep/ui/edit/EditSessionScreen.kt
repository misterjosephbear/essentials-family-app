package com.isaacshub.app.sleep.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionScreen(
    sessionId: Long?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: EditSessionViewModel = viewModel(
        factory = EditSessionViewModel.Factory(app.sleepRepository, sessionId)
    )
    val state by viewModel.uiState.collectAsState()

    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (sessionId == null) "Add sleep" else "Edit sleep") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = { editingStart = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Start: ${formatInstant(state.start)}")
            }
            OutlinedButton(onClick = { editingEnd = true }, modifier = Modifier.fillMaxWidth()) {
                Text("End: ${formatInstant(state.end)}")
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }

    if (editingStart) {
        InstantPickerDialog(
            initial = state.start,
            onConfirm = {
                viewModel.setStart(it)
                editingStart = false
            },
            onDismiss = { editingStart = false }
        )
    }
    if (editingEnd) {
        InstantPickerDialog(
            initial = state.end,
            onConfirm = {
                viewModel.setEnd(it)
                editingEnd = false
            },
            onDismiss = { editingEnd = false }
        )
    }
}

private val displayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a")

private fun formatInstant(instant: Instant): String =
    instant.atZone(ZoneId.systemDefault()).format(displayFormatter)

/**
 * Picks a date then a time as two separate dialogs rather than stacking both pickers in one -
 * DatePickerDialog is sized just for a DatePicker's calendar grid, so adding a full TimePicker
 * clock face below it pushed the clock face past the dialog's visible bounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstantPickerDialog(
    initial: Instant,
    onConfirm: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val zoned = initial.atZone(zone)
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    val date = pickedDate
    if (date == null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = zoned.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    pickedDate = if (selectedMillis != null) {
                        Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        zoned.toLocalDate()
                    }
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
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = zoned.hour,
            initialMinute = zoned.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val result = date.atTime(timePickerState.hour, timePickerState.minute).atZone(zone).toInstant()
                    onConfirm(result)
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
}

package com.isaacshub.app.sleep.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.sleep.detection.SleepDetectionController
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.preferencesRepository)
    )
    val prefs by viewModel.preferences.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setAutoDetectEnabled(true)
            SleepDetectionController.start(context)
        }
    }

    var sliderValue by remember(prefs.sleepNeedMinutes) { mutableFloatStateOf(prefs.sleepNeedMinutes.toFloat()) }
    var editingWakeDay by remember { mutableStateOf<DayOfWeek?>(null) }

    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoringOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Sleep tracking", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-detect sleep")
                Text(
                    "Uses screen and motion signals to detect sleep automatically",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = prefs.autoDetectEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setAutoDetectEnabled(true)
                            SleepDetectionController.start(context)
                        }
                    } else {
                        viewModel.setAutoDetectEnabled(false)
                        SleepDetectionController.stop(context)
                    }
                }
            )
        }

        Column {
            Text("Sleep need: ${sliderValue.toInt() / 60}h ${sliderValue.toInt() % 60}m")
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.setSleepNeedMinutes(sliderValue.toInt()) },
                valueRange = 360f..600f,
                steps = 23
            )
        }

        var debtAdjustmentValue by remember(prefs.sleepDebtAdjustmentMinutes) { mutableFloatStateOf(prefs.sleepDebtAdjustmentMinutes.toFloat()) }
        Column {
            val hours = debtAdjustmentValue.toInt() / 60
            val mins = kotlin.math.abs(debtAdjustmentValue.toInt() % 60)
            val sign = if (debtAdjustmentValue >= 0) "+" else "-"
            Text("Sleep debt adjustment: $sign${kotlin.math.abs(hours)}h ${mins}m")
            Text(
                "Manually add or subtract from calculated sleep debt",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = debtAdjustmentValue,
                onValueChange = { debtAdjustmentValue = it },
                onValueChangeFinished = { viewModel.setSleepDebtAdjustment(debtAdjustmentValue.toInt()) },
                valueRange = -480f..480f,
                steps = 31
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Wake time by day", style = MaterialTheme.typography.titleMedium)
            Text(
                "Used to work out when to start winding down each evening.",
                style = MaterialTheme.typography.bodySmall
            )
            DayOfWeek.entries.forEach { day ->
                val minutes = prefs.wakeTimeMinutesByDay.getValue(day)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    TextButton(onClick = { editingWakeDay = day }) {
                        Text(formatMinutesOfDay(minutes))
                    }
                }
            }
        }

        if (!ignoringOptimizations) {
            Column {
                OutlinedButton(onClick = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) {
                    Text("Improve detection reliability")
                }
                Text(
                    "Sleep detection runs more reliably if battery optimization is disabled for this app.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    editingWakeDay?.let { day ->
        val minutes = prefs.wakeTimeMinutesByDay.getValue(day)
        WakeTimePickerDialog(
            initial = LocalTime.of(minutes / 60, minutes % 60),
            onConfirm = { time ->
                viewModel.setWakeTime(day, time.hour * 60 + time.minute)
                editingWakeDay = null
            },
            onDismiss = { editingWakeDay = null }
        )
    }
}

private val wakeTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")

private fun formatMinutesOfDay(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(wakeTimeFormatter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WakeTimePickerDialog(
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

package com.isaacshub.app.sleep.ui.nap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.sleep.nap.NapAlarmScheduler
import com.isaacshub.app.sleep.nap.NapPhase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val napDurations = listOf(
    10, 15, 20, 30, 45, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 360, 390, 420, 450, 480, 510, 540, 570, 600, 630, 660, 690, 720
)
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NapScreen() {
    val context = LocalContext.current
    val viewModel: NapViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    var selectedMinutes by remember { mutableIntStateOf(20) }
    var canScheduleExact by remember { mutableIntStateOf(if (NapAlarmScheduler.canScheduleExact(context)) 1 else 0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.startNap(selectedMinutes) }

    fun beginNap() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startNap(selectedMinutes)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nap alarm") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state.phase) {
                NapPhase.IDLE -> {
                    Text("How long do you want to nap?", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(napDurations) { minutes ->
                            FilterChip(
                                selected = selectedMinutes == minutes,
                                onClick = { selectedMinutes = minutes },
                                label = { Text(formatDuration(minutes)) }
                            )
                        }
                    }
                    Button(onClick = ::beginNap, modifier = Modifier.fillMaxWidth()) {
                        Text("Start ${formatDuration(selectedMinutes)} nap")
                    }

                    // Show either the "Start Sleep" button or confirmation card
                    if (!state.eveningSleepScheduled) {
                        OutlinedButton(
                            onClick = { viewModel.startSleep() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Sleep for Evening")
                        }
                    } else {
                        // Show confirmation card with cancel button
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "✓ Sleep scheduled for evening",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                state.eveningSleepWakeTime?.let { wakeTime ->
                                    Text(
                                        "Wake time: ${wakeTime.atZone(ZoneId.systemDefault()).format(timeFormatter)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelEveningSleep() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel Evening Sleep")
                                }
                            }
                        }
                    }

                    if (canScheduleExact == 0) {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Without exact alarm permission, your nap alarm may fire a few minutes late.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                    canScheduleExact = if (NapAlarmScheduler.canScheduleExact(context)) 1 else 0
                                }) {
                                    Text("Allow exact alarms")
                                }
                            }
                        }
                    }
                }

                NapPhase.NAPPING -> {
                    Text("Napping", style = MaterialTheme.typography.titleMedium)
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Started at ${formatEpochMillis(state.startEpochMillis)}")
                            Text("Alarm at ${formatEpochMillis(state.alarmEpochMillis)}")
                        }
                    }
                    OutlinedButton(onClick = { viewModel.cancelNap() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel nap")
                    }
                }

                NapPhase.RINGING -> {
                    Text("Time to wake up!", style = MaterialTheme.typography.headlineSmall)
                    Text("Your nap will be logged when you stop the alarm.")
                    Button(onClick = { viewModel.stopAlarm() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Stop alarm")
                    }
                }
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun formatEpochMillis(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

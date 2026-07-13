package com.isaacshub.app.sleep.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.sleep.ui.components.EnergyForecastChart
import com.isaacshub.app.sleep.ui.components.SleepDebtRing
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onEditSession: (Long?) -> Unit,
    onAddSession: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.sleepRepository, app.preferencesRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSession) {
                Icon(Icons.Filled.Add, contentDescription = "Add sleep session")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SleepDebtRing(
                        debtMinutes = state.debtMinutes,
                        capMinutes = state.sleepNeedMinutes * 3
                    )
                }
            }

            state.pendingConfirmation?.let { pending ->
                item {
                    Card(onClick = { onEditSession(pending.id) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sleep detected", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatRange(pending.startEpochMillis, pending.endEpochMillis)} - tap to confirm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Text("Last night", style = MaterialTheme.typography.titleMedium)
            }
            item {
                val lastNight = state.lastNight
                if (lastNight == null) {
                    Text("No sleep logged yet. Add one with the + button.")
                } else {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                formatRange(lastNight.startEpochMillis, lastNight.endEpochMillis),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                formatDuration(lastNight.startEpochMillis, lastNight.endEpochMillis),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            state.energyForecast?.let { forecast ->
                item {
                    Text("Today's energy", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            EnergyForecastChart(
                                forecast = forecast,
                                currentHoursAwake = state.currentHoursAwake
                            )
                            Text(
                                "Estimated from your wake time and sleep debt - not a clinical measurement.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private val rangeDateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private val rangeTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatRange(startMillis: Long, endMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(endMillis).atZone(zone)
    return "${start.format(rangeDateFormatter)} - ${end.format(rangeTimeFormatter)}"
}

private fun formatDuration(startMillis: Long, endMillis: Long): String {
    val duration = Duration.ofMillis(endMillis - startMillis)
    return "${duration.toHours()}h ${duration.toMinutes() % 60}m"
}

package com.isaacshub.app.timetracking.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import com.isaacshub.app.timetracking.ui.components.PayPeriodSummaryCard
import com.isaacshub.app.timetracking.ui.components.WeeklyHoursBar
import com.isaacshub.app.timetracking.ui.formatNumber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimeTrackingHomeScreen(
    onEditEntry: (Long?) -> Unit,
    onAddEntry: () -> Unit,
    onOpenSchedule: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: TimeTrackingHomeViewModel = viewModel(
        factory = TimeTrackingHomeViewModel.Factory(app.timeTrackingRepository, app.preferencesRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = "Add time entry")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card {
                    Box(modifier = Modifier.padding(16.dp)) {
                        PayPeriodSummaryCard(summary = state.payPeriod)
                    }
                }
            }
            item {
                Card(onClick = onOpenSchedule) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        WeeklyHoursBar(summary = state.summary)
                    }
                }
            }
            item {
                Text("This week's entries", style = MaterialTheme.typography.titleMedium)
            }
            if (state.summary.entries.isEmpty()) {
                item { Text("No entries logged yet this week. Add one with the + button.") }
            } else {
                items(state.summary.entries, key = { it.id }) { entry ->
                    TimeEntryRow(
                        entry = entry,
                        onClick = { onEditEntry(entry.id) },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeEntryRow(
    entry: TimeEntryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val date = Instant.ofEpochMilli(entry.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    val detail = when (entry.payType) {
        PayType.HOURLY -> "${formatNumber(entry.actualHours)}h worked"
        PayType.EVALUATION ->
            "${formatNumber(entry.actualHours)}h worked - ${formatNumber(entry.evaluatedHours ?: 0.0)} eval hrs - " +
                "${formatNumber(entry.evaluatedMiles ?: 0.0)} mi"
    }

    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(entry.label, style = MaterialTheme.typography.titleMedium)
                Text(date.format(dateFormatter), style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (entry.payType == PayType.HOURLY) "Hourly" else "Evaluation") }
                    )
                    Text(detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

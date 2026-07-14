package com.isaacshub.app.sleep.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.isaacshub.app.sleep.data.SleepSessionEntity
import com.isaacshub.app.sleep.data.SleepSource
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    onEditSession: (Long?) -> Unit,
    onAddSession: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(app.sleepRepository))
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSession) {
                Icon(Icons.Filled.Add, contentDescription = "Add sleep session")
            }
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No sleep sessions yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onClick = { onEditSession(session.id) },
                        onDelete = { viewModel.delete(session) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SleepSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(session.startEpochMillis).atZone(zone)
    val end = Instant.ofEpochMilli(session.endEpochMillis).atZone(zone)
    val duration = Duration.ofMillis(session.endEpochMillis - session.startEpochMillis)
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val sourceLabel = when {
        !session.confirmed -> " - unconfirmed"
        session.source == SleepSource.AUTO_DETECTED -> " - auto"
        session.source == SleepSource.NAP -> " - nap"
        else -> " - manual"
    }

    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(end.format(dateFormatter), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${start.format(timeFormatter)} - ${end.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${duration.toHours()}h ${duration.toMinutes() % 60}m$sourceLabel",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

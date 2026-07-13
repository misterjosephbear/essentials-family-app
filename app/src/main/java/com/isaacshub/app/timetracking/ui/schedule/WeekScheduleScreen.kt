package com.isaacshub.app.timetracking.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.timetracking.domain.ScheduledDay
import com.isaacshub.app.timetracking.domain.ScheduledRouteStatus
import com.isaacshub.app.timetracking.ui.formatNumber
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScheduleScreen(
    onEditEntry: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: WeekScheduleViewModel = viewModel(
        factory = WeekScheduleViewModel.Factory(app.timeTrackingRepository)
    )
    val days by viewModel.days.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("This week's schedule") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days, key = { it.date }) { day ->
                DayCard(day = day, onEditEntry = onEditEntry)
            }
        }
    }
}

@Composable
private fun DayCard(
    day: ScheduledDay,
    onEditEntry: (Long) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(day.date.format(dayFormatter), style = MaterialTheme.typography.titleMedium)
            if (day.routes.isEmpty()) {
                Text(
                    "No scheduled routes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                day.routes.forEach { status ->
                    RouteStatusRow(status = status, onClick = {
                        status.loggedEntry?.let { onEditEntry(it.id) }
                    })
                }
            }
        }
    }
}

@Composable
private fun RouteStatusRow(
    status: ScheduledRouteStatus,
    onClick: () -> Unit
) {
    val route = status.route
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("${route.routeNumber} - ${route.cityName}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${formatNumber(route.evaluatedHours)} eval hrs - ${formatNumber(route.evaluatedMiles)} mi",
                style = MaterialTheme.typography.bodySmall
            )
        }
        AssistChip(
            onClick = onClick,
            label = { Text(if (status.isLogged) "Logged" else "Scheduled") },
            colors = if (status.isLogged) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                AssistChipDefaults.assistChipColors()
            }
        )
    }
}

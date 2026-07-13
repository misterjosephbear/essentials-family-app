package com.isaacshub.app.timetracking.ui.schedule

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.domain.ScheduledDay
import com.isaacshub.app.timetracking.domain.ScheduledRouteStatus
import com.isaacshub.app.timetracking.ui.components.TimeEntryRow
import com.isaacshub.app.timetracking.ui.components.WeeklyHoursBar
import com.isaacshub.app.timetracking.ui.formatNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val weekRangeFormatter = DateTimeFormatter.ofPattern("MMM d")

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
    val state by viewModel.uiState.collectAsState()
    val summary = state.summary
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${summary.weekStart.format(weekRangeFormatter)} - " +
                            summary.weekEnd.format(weekRangeFormatter)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add one-time schedule")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::previousWeek) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous week")
                    }
                    if (state.weekOffset == 0) {
                        Text("This week", style = MaterialTheme.typography.titleMedium)
                    } else {
                        TextButton(onClick = viewModel::goToCurrentWeek) {
                            Text("Back to this week")
                        }
                    }
                    IconButton(onClick = viewModel::nextWeek) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next week")
                    }
                }
            }
            item {
                val title = when {
                    state.weekOffset < 0 -> "Hours that week"
                    state.weekOffset > 0 -> "Projected hours"
                    else -> "This week's projected hours"
                }
                Card {
                    Box(modifier = Modifier.padding(16.dp)) {
                        WeeklyHoursBar(summary = summary, title = title)
                    }
                }
            }
            item {
                Text("Schedule", style = MaterialTheme.typography.titleMedium)
            }
            items(state.days, key = { it.date }) { day ->
                DayCard(day = day, onEditEntry = onEditEntry, onRemoveOverride = viewModel::removeScheduleOverride)
            }
            item {
                Text("Entries", style = MaterialTheme.typography.titleMedium)
            }
            if (summary.entries.isEmpty()) {
                item { Text("No entries logged this week.") }
            } else {
                items(summary.entries, key = { it.id }) { entry ->
                    TimeEntryRow(entry = entry, onClick = { onEditEntry(entry.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleOverrideDialog(
            weekStart = summary.weekStart,
            routes = state.routes,
            onConfirm = { routeId, dates ->
                viewModel.addScheduleOverrides(routeId, dates)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun DayCard(
    day: ScheduledDay,
    onEditEntry: (Long) -> Unit,
    onRemoveOverride: (Long) -> Unit
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
                    RouteStatusRow(
                        status = status,
                        onClick = { status.loggedEntry?.let { onEditEntry(it.id) } },
                        onRemoveOverride = onRemoveOverride
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteStatusRow(
    status: ScheduledRouteStatus,
    onClick: () -> Unit,
    onRemoveOverride: (Long) -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${route.routeNumber} - ${route.cityName}", style = MaterialTheme.typography.bodyLarge)
                if (status.isOneOff) {
                    AssistChip(onClick = {}, label = { Text("One-time") })
                }
            }
            Text(
                "${formatNumber(route.evaluatedHours)} eval hrs - ${formatNumber(route.evaluatedMiles)} mi",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            if (status.isOneOff) {
                IconButton(onClick = { onRemoveOverride(status.overrideId!!) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove one-time schedule")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleOverrideDialog(
    weekStart: LocalDate,
    routes: List<RouteEntity>,
    onConfirm: (routeId: Long, dates: List<LocalDate>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRouteId by remember { mutableStateOf(routes.firstOrNull()?.id) }
    val selectedDates = remember { mutableStateOf(setOf<LocalDate>()) }
    var routeExpanded by remember { mutableStateOf(false) }
    val weekDates = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add one-time schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (routes.isEmpty()) {
                    Text("Add a route first from the Routes tab.")
                } else {
                    val selectedLabel = routes.firstOrNull { it.id == selectedRouteId }
                        ?.let { "${it.routeNumber} - ${it.cityName}" } ?: "Select a route"
                    ExposedDropdownMenuBox(expanded = routeExpanded, onExpandedChange = { routeExpanded = it }) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Route") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                            routes.forEach { route ->
                                DropdownMenuItem(
                                    text = { Text("${route.routeNumber} - ${route.cityName}") },
                                    onClick = {
                                        selectedRouteId = route.id
                                        routeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text("Days", style = MaterialTheme.typography.labelLarge)
                    weekDates.forEach { date ->
                        val checked = date in selectedDates.value
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedDates.value = if (isChecked) {
                                        selectedDates.value + date
                                    } else {
                                        selectedDates.value - date
                                    }
                                }
                            )
                            Text(date.format(dayFormatter))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val routeId = selectedRouteId
                    if (routeId != null && selectedDates.value.isNotEmpty()) {
                        onConfirm(routeId, selectedDates.value.toList())
                    }
                },
                enabled = selectedRouteId != null && selectedDates.value.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

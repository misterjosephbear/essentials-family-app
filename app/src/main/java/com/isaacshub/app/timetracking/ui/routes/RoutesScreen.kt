package com.isaacshub.app.timetracking.ui.routes

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
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.ui.formatNumber

@Composable
fun RoutesScreen(
    onEditRoute: (Long?) -> Unit,
    onAddRoute: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: RoutesViewModel = viewModel(factory = RoutesViewModel.Factory(app.timeTrackingRepository))
    val routes by viewModel.routes.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoute) {
                Icon(Icons.Filled.Add, contentDescription = "Add route")
            }
        }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No saved routes yet. Add one with the + button for quick input.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteRow(
                        route = route,
                        onClick = { onEditRoute(route.id) },
                        onDelete = { viewModel.delete(route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteRow(
    route: RouteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${route.routeNumber} - ${route.cityName}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatNumber(route.evaluatedHours)} eval hrs - ${formatNumber(route.evaluatedMiles)} mi",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

package com.isaacshub.app.routehelper.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isaacshub.app.routehelper.data.RoutedStopEntity

/**
 * Amazon Load Truck screen - displays stops with adjustable package quantities.
 * This replaces the package scanner for Amazon routes since we already know the
 * expected package count from the direction sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmazonLoadTruckScreen(
    stops: List<RoutedStopEntity>,
    onUpdateQuantity: (stopId: Long, newQuantity: Int) -> Unit,
    onDone: () -> Unit
) {
    // Track local quantity adjustments
    var quantities by remember {
        mutableStateOf(stops.associate { it.id to (it.expectedPackageCount ?: 0) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Load Truck") },
                actions = {
                    TextButton(onClick = {
                        // Save all quantity changes
                        quantities.forEach { (stopId, qty) ->
                            onUpdateQuantity(stopId, qty)
                        }
                        onDone()
                    }) {
                        Text("Done")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Adjust package quantities for each stop:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stops) { stop ->
                    val currentQty = quantities[stop.id] ?: 0

                    LoadTruckStopItem(
                        stop = stop,
                        quantity = currentQty,
                        onIncrement = {
                            quantities = quantities + (stop.id to (currentQty + 1))
                        },
                        onDecrement = {
                            if (currentQty > 0) {
                                quantities = quantities + (stop.id to (currentQty - 1))
                            }
                        }
                    )
                }
            }

            // Summary at bottom
            val totalPackages = quantities.values.sum()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total Packages:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$totalPackages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadTruckStopItem(
    stop: RoutedStopEntity,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${stop.sequenceOrder + 1}. ${stop.addressLabel}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (stop.expectedPackageCount != null && stop.expectedPackageCount != quantity) {
                    Text(
                        "Expected: ${stop.expectedPackageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onDecrement) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Decrease quantity",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(onClick = onIncrement) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Increase quantity",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

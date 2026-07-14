package com.isaacshub.app.routehelper.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.ui.common.AddressActionCard
import com.isaacshub.app.routehelper.ui.common.newOsmMapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/**
 * A debug-only sandbox for the route builder: enter a ZIP to zoom the map there and fetch its real
 * OSM addresses, then tap anywhere on the map to fake being at that spot and see the same nearest-5
 * list and one-tap Left/Right/In Drive/plain buttons the live builder uses. Everything routed here
 * lives only in [RouteHelperTestViewModel]'s in-memory state - it's never written to the database,
 * so there's nothing to clean up and it can never show up as a real route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteHelperTestScreen() {
    val viewModel: RouteHelperTestViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Testing Mode") },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.testStops.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last test stop")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.zipInput,
                    onValueChange = viewModel::onZipInputChange,
                    label = { Text("ZIP code") },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::loadZip, enabled = !state.loading) {
                    Text("Go")
                }
            }

            if (state.loading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Downloading addresses for this ZIP...")
                }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }

            TestMap(state = state, onMapTap = viewModel::setFakeLocation, modifier = Modifier.fillMaxWidth().height(220.dp))

            Text(
                if (state.mapCenter == null) "Enter a ZIP to start" else "Test stops routed: ${state.testStops.size} (not saved)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.mapCenter != null) {
                Text(
                    "Tap the map to fake your location.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.nearestAddresses.isEmpty()) {
                if (state.mapCenter != null) {
                    Text(
                        "No nearby unrouted addresses.",
                        modifier = Modifier.weight(1f).padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.nearestAddresses, key = { it.id }) { candidate ->
                        AddressActionCard(
                            label = candidate.label,
                            onTap = { side -> viewModel.routeStop(candidate, side) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestMap(state: RouteHelperTestUiState, onMapTap: (GeoPoint) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var lastCenter by remember { mutableStateOf<GeoPoint?>(null) }

    val mapView = remember { newOsmMapView(context) }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    AndroidView(modifier = modifier, factory = { mapView }) { view ->
        view.overlays.clear()

        view.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmGeoPoint): Boolean {
                    onMapTap(GeoPoint(p.latitude, p.longitude))
                    return true
                }

                override fun longPressHelper(p: OsmGeoPoint): Boolean = false
            })
        )

        state.mapCenter?.let { center ->
            if (lastCenter != center) {
                view.controller.setZoom(16.0)
                view.controller.setCenter(OsmGeoPoint(center.latitude, center.longitude))
                lastCenter = center
            }
        }

        state.fakeLocation?.let { location ->
            view.overlays.add(
                Marker(view).apply {
                    position = OsmGeoPoint(location.latitude, location.longitude)
                    title = "Fake location"
                    snippet = "Tap elsewhere on the map to move it"
                }
            )
        }

        state.nearestAddresses.forEach { candidate ->
            view.overlays.add(
                Marker(view).apply {
                    position = OsmGeoPoint(candidate.location.latitude, candidate.location.longitude)
                    title = candidate.label
                }
            )
        }

        state.testStops.forEachIndexed { index, stop ->
            view.overlays.add(
                Marker(view).apply {
                    position = OsmGeoPoint(stop.location.latitude, stop.location.longitude)
                    title = "${index + 1}. ${stop.label}"
                    snippet = stop.note ?: "No note"
                }
            )
        }

        view.invalidate()
    }
}

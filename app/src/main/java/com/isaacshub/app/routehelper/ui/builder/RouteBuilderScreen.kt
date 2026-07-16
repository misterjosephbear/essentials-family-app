package com.isaacshub.app.routehelper.ui.builder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.routehelper.ui.common.AddressActionCard
import com.isaacshub.app.routehelper.ui.common.newOsmMapView
import com.isaacshub.app.routehelper.ui.player.MailScanScreen
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/** Height reserved for an empty address slot so it holds its place in the layout, matching a populated [AddressActionCard]. */
private val ADDRESS_SLOT_MIN_HEIGHT = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteBuilderScreen(routeId: Long) {
    val context = LocalContext.current
    val viewModel: RouteBuilderViewModel = viewModel()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.start(routeId)
    }

    val state by viewModel.uiState.collectAsState()
    var isScanning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Building route") },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last stop")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isScanning = true }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Scan mail piece")
            }
        }
    ) { padding ->
        if (!hasLocationPermission) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Location access is needed to build a route.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LiveMap(state = state, modifier = Modifier.fillMaxWidth().height(220.dp).clipToBounds())

            Text(
                "Stops routed: ${state.routedStops.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.nearestAddressSlots.all { it == null }) {
                Text(
                    "No nearby unrouted addresses.",
                    modifier = Modifier.weight(1f).padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Each slot is a fixed screen position - reassigned candidates crossfade in place
                    // rather than the whole list reordering, so a tap target never moves under a driver's thumb.
                    state.nearestAddressSlots.forEach { slotCandidate ->
                        Crossfade(targetState = slotCandidate, label = "addressSlot") { candidate ->
                            if (candidate != null) {
                                AddressActionCard(
                                    label = candidate.label,
                                    isPending = candidate.id in state.pendingCandidateIds,
                                    onTap = { side -> viewModel.routeStop(candidate, side) }
                                )
                            } else {
                                Spacer(modifier = Modifier.fillMaxWidth().height(ADDRESS_SLOT_MIN_HEIGHT))
                            }
                        }
                    }
                }
            }
        }
    }

    if (isScanning) {
        Dialog(onDismissRequest = { isScanning = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            MailScanScreen(
                currentLocation = state.currentLocation,
                routeZip = state.routeZip,
                currentRoadName = null,
                onResolved = { resolved ->
                    viewModel.addScannedStop(resolved)
                    isScanning = false
                },
                onCancel = { isScanning = false }
            )
        }
    }
}

@Composable
private fun LiveMap(state: RouteBuilderUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCenteredOnce by remember { mutableStateOf(false) }

    val mapView = remember { newOsmMapView(context) }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    AndroidView(modifier = modifier, factory = { mapView }) { view ->
        view.overlays.clear()

        state.currentLocation?.let { location ->
            val point = OsmGeoPoint(location.latitude, location.longitude)
            if (!hasCenteredOnce) {
                view.controller.setCenter(point)
                hasCenteredOnce = true
            } else {
                view.controller.animateTo(point)
            }
            view.overlays.add(
                Marker(view).apply {
                    position = point
                    title = "You"
                }
            )
        }

        state.nearestAddressSlots.filterNotNull().forEach { candidate ->
            view.overlays.add(
                Marker(view).apply {
                    position = OsmGeoPoint(candidate.latitude, candidate.longitude)
                    title = candidate.label
                }
            )
        }

        state.routedStops.forEach { stop ->
            view.overlays.add(
                Marker(view).apply {
                    position = OsmGeoPoint(stop.latitude, stop.longitude)
                    title = "${stop.sequenceOrder + 1}. ${stop.addressLabel}"
                }
            )
        }

        view.invalidate()
    }
}

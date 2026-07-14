package com.isaacshub.app.routehelper.ui.builder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.routehelper.data.CandidateAddressEntity
import com.isaacshub.app.routehelper.domain.StopSide
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import org.osmdroid.util.GeoPoint as OsmGeoPoint

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
        }
    ) { padding ->
        if (!hasLocationPermission) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Location access is needed to build a route.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LiveMap(state = state, modifier = Modifier.fillMaxWidth().height(220.dp))

            Text(
                "Stops routed: ${state.routedStops.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.nearestAddresses.isEmpty()) {
                Text(
                    "No nearby unrouted addresses.",
                    modifier = Modifier.weight(1f).padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.nearestAddresses, key = { it.id }) { candidate ->
                        AddressCard(
                            candidate = candidate,
                            onTap = { side -> viewModel.routeStop(candidate, side) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressCard(candidate: CandidateAddressEntity, onTap: (StopSide) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTap(StopSide.NONE) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    candidate.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onTap(StopSide.LEFT) }, modifier = Modifier.weight(1f)) {
                    Text("Left")
                }
                Button(onClick = { onTap(StopSide.RIGHT) }, modifier = Modifier.weight(1f)) {
                    Text("Right")
                }
                Button(onClick = { onTap(StopSide.IN_DRIVE) }, modifier = Modifier.weight(1f)) {
                    Text("In Drive")
                }
            }
        }
    }
}

@Composable
private fun LiveMap(state: RouteBuilderUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCenteredOnce by remember { mutableStateOf(false) }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = File(context.filesDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(context.filesDir, "osmdroid/tiles")
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
    }

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

        state.nearestAddresses.forEach { candidate ->
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

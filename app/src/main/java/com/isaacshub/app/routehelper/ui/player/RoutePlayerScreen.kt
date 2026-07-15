package com.isaacshub.app.routehelper.ui.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.debug.DebugLogger
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.ui.common.newOsmMapView
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/**
 * The live "GPS" screen for a route already built with the route builder: rotates the map so the
 * driver's direction of travel always faces up, draws the whole recorded route as a line from stop to
 * stop, and surfaces which stop is next in a small overlay. Package info is a placeholder until that
 * feature exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlayerScreen(routeId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val viewModel: RoutePlayerViewModel = viewModel()

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
    var isFreeCam by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driving route") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit route player")
                    }
                },
                actions = {
                    // Debug button to send logs to server
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Sending debug logs...")
                            val success1 = DebugLogger.sendLogsToServer(context, "Route Player Logs")
                            val success2 = DebugLogger.sendRouteDebugInfo(
                                routeId = routeId,
                                stopCount = state.stops.size,
                                roadRoutePointCount = state.roadRoutePoints?.size,
                                debugInfo = state.roadRouteDebugInfo
                            )
                            if (success1 && success2) {
                                snackbarHostState.showSnackbar("Debug logs sent successfully!")
                            } else {
                                snackbarHostState.showSnackbar("Failed to send logs. Check network connection.")
                            }
                        }
                    }) {
                        Icon(Icons.Filled.BugReport, contentDescription = "Send debug logs to server")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (!hasLocationPermission) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Location access is needed to drive this route.")
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PlayerMap(
                state = state,
                isFreeCam = isFreeCam,
                modifier = Modifier.fillMaxSize().clipToBounds()
            )

            val nextStop = state.nextStopIndex?.let { state.stops.getOrNull(it) }
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when {
                        state.stops.isEmpty() -> Text(
                            "This route has no stops yet.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        nextStop != null -> {
                            Text("Next stop: ${nextStop.addressLabel}", style = MaterialTheme.typography.titleMedium)
                            Text("Package: feature coming soon", style = MaterialTheme.typography.bodySmall)
                        }
                        else -> Text("Route complete!", style = MaterialTheme.typography.titleMedium)
                    }
                    // Debug info for road route status
                    if (state.roadRouteDebugInfo.isNotEmpty()) {
                        Text(
                            "Road route: ${state.roadRouteDebugInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Bottom-right FAB row: GPS toggle and mail scanner
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                // GPS lock/unlock toggle
                FloatingActionButton(
                    onClick = { isFreeCam = !isFreeCam }
                ) {
                    Icon(
                        if (isFreeCam) Icons.Filled.GpsNotFixed else Icons.Filled.GpsFixed,
                        contentDescription = if (isFreeCam) "Enable GPS tracking" else "Disable GPS tracking"
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Mail scanner
                FloatingActionButton(
                    onClick = { isScanning = true }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Scan a mail piece to add a stop")
                }
            }
        }
    }

    if (isScanning) {
        Dialog(onDismissRequest = { isScanning = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            MailScanScreen(
                currentLocation = state.currentLocation,
                onResolved = { resolved ->
                    viewModel.addScannedStop(resolved)
                    isScanning = false
                },
                onCancel = { isScanning = false }
            )
        }
    }
}

private val routeLineColor = Color(0xFF1A73E8).toArgb()

@Composable
private fun PlayerMap(state: RoutePlayerUiState, isFreeCam: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCenteredOnce by remember { mutableStateOf(false) }
    val mapView = remember { newOsmMapView(context) }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    AndroidView(modifier = modifier, factory = { mapView }) { view ->
        view.overlays.clear()

        // Only rotate map and follow GPS when free-cam is disabled
        if (!isFreeCam) {
            // Negated so the driver's live bearing visually points to the top of the screen (course-up),
            // rather than the map staying north-up like the builder's preview map.
            view.setMapOrientation(-state.mapBearingDegrees, false)
        } else {
            // In free-cam mode, keep map north-up
            view.setMapOrientation(0f, false)
        }

        // Prefer the real road-following path; fall back to a straight line between stops if it hasn't
        // loaded yet (or failed - no signal, etc.) so the driver still sees a path either way.
        val routeLine = state.roadRoutePoints ?: state.stops.map { GeoPoint(it.latitude, it.longitude) }
        if (routeLine.size >= 2) {
            view.overlays.add(
                Polyline(view).apply {
                    setPoints(routeLine.map { OsmGeoPoint(it.latitude, it.longitude) })
                    outlinePaint.color = routeLineColor
                    outlinePaint.strokeWidth = 10f
                }
            )
        }

        state.stops.forEachIndexed { index, stop ->
            view.overlays.add(
                Marker(view).apply {
                    position = OsmGeoPoint(stop.latitude, stop.longitude)
                    title = "${index + 1}. ${stop.addressLabel}"
                }
            )
        }

        state.currentLocation?.let { location ->
            val point = OsmGeoPoint(location.latitude, location.longitude)

            // Only auto-center and follow GPS when free-cam is disabled
            if (!isFreeCam) {
                if (!hasCenteredOnce) {
                    view.controller.setZoom(18.0)
                    view.controller.setCenter(point)
                    hasCenteredOnce = true
                } else {
                    view.controller.animateTo(point)
                }
            }

            // Always show the "You" marker regardless of free-cam mode
            view.overlays.add(
                Marker(view).apply {
                    position = point
                    title = "You"
                }
            )
        }

        view.invalidate()
    }
}

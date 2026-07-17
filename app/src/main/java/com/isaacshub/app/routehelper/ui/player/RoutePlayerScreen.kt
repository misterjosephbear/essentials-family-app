package com.isaacshub.app.routehelper.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.isaacshub.app.routehelper.domain.distanceMeters
import com.isaacshub.app.routehelper.domain.offsetPolylineRight
import com.isaacshub.app.routehelper.ui.common.newOsmMapView
import kotlinx.coroutines.launch
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import kotlin.math.pow

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
    var isScanningPackages by remember { mutableStateOf(false) }
    var isFreeCam by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Keep screen awake during route playback
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

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
                    // Package scanner button
                    IconButton(onClick = { isScanningPackages = true }) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = "Scan packages")
                    }

                    // Debug button to send logs to server
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                snackbarHostState.showSnackbar("Sending logs...")
                                val success1 = DebugLogger.sendLogsToServer(context, "Route Player Logs")
                                val success2 = DebugLogger.sendRouteDebugInfo(
                                    routeId = routeId,
                                    stopCount = state.stops.size,
                                    roadRoutePointCount = state.roadRoutePoints?.size,
                                    debugInfo = state.roadRouteDebugInfo
                                )
                                if (success1 && success2) {
                                    snackbarHostState.showSnackbar("✓ Logs sent successfully!", duration = androidx.compose.material3.SnackbarDuration.Short)
                                } else {
                                    snackbarHostState.showSnackbar("✗ Failed to send logs", duration = androidx.compose.material3.SnackbarDuration.Long)
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}", duration = androidx.compose.material3.SnackbarDuration.Long)
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
                        state.isAtStop && state.clusterStops.isNotEmpty() -> {
                            // Currently stopped at a stop
                            Text("Currently at:", style = MaterialTheme.typography.titleMedium)
                            state.clusterStops.forEach { stop ->
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stop.addressLabel,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val packageCount = state.packageCountsByStop[stop.id] ?: 0
                                    if (packageCount > 0) {
                                        Text(
                                            " 📦×$packageCount",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                stop.note?.let { note ->
                                    Text(
                                        note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                        state.clusterStops.size > 1 -> {
                            // Multiple stops clustered together
                            Text("Next stops:", style = MaterialTheme.typography.titleMedium)
                            state.clusterStops.forEach { stop ->
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stop.addressLabel,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val packageCount = state.packageCountsByStop[stop.id] ?: 0
                                    if (packageCount > 0) {
                                        Text(
                                            " 📦×$packageCount",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                stop.note?.let { note ->
                                    Text(
                                        note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                        state.clusterStops.size == 1 -> {
                            // Single next stop
                            val nextStop = state.clusterStops.first()
                            val packageCount = state.packageCountsByStop[nextStop.id] ?: 0
                            val nextPackageAddress = state.nextPackageAddressByStop[nextStop.id]

                            val stopText = if (packageCount > 0) {
                                "${nextStop.addressLabel} 📦×$packageCount"
                            } else {
                                nextStop.addressLabel
                            }
                            Text("Next stop: $stopText", style = MaterialTheme.typography.titleMedium)

                            // Show next package address if available
                            nextPackageAddress?.let { pkgAddr ->
                                Text(
                                    "Next package: $pkgAddr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            nextStop.note?.let { note ->
                                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
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
                routeZip = state.routeZip,
                currentRoadName = state.currentRoadName,
                onResolved = { resolved ->
                    viewModel.addScannedStop(resolved)
                    isScanning = false
                },
                onCancel = { isScanning = false }
            )
        }
    }

    if (isScanningPackages) {
        val scannedPackages by viewModel.observePackages().collectAsState(initial = emptyList())
        Dialog(onDismissRequest = { isScanningPackages = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            PackageScanScreen(
                routeId = routeId,
                scannedPackages = scannedPackages.map { pkg ->
                    ScannedPackage(pkg.trackingNumber, pkg.addressLabel)
                },
                onPackageScanned = { pkg ->
                    viewModel.addPackage(pkg.trackingNumber, pkg.addressLabel)
                },
                onPackageDeleted = { pkg ->
                    // Find the matching package entity and delete it
                    scannedPackages.find { it.trackingNumber == pkg.trackingNumber }?.let {
                        viewModel.deletePackage(it)
                    }
                },
                onDone = { isScanningPackages = false }
            )
        }
    }
}

private val routeLineColor = Color(0xFF1A73E8).toArgb()

/**
 * Calculate scaling factors for map elements based on zoom level.
 * As you zoom out (lower zoom), elements scale up to remain visible.
 *
 * @param zoomLevel Current map zoom level (typically 1-21)
 * @return Scaling factor (1.0 at zoom 18, increases as zoom decreases)
 */
private fun calculateScaleFactor(zoomLevel: Double): Float {
    // Reference zoom level where scale = 1.0
    val referenceZoom = 18.0
    // Scale increases as zoom decreases (zoom out)
    // Formula: scale = 2^(referenceZoom - currentZoom)
    return 2.0.pow((referenceZoom - zoomLevel).coerceAtLeast(0.0)).toFloat().coerceAtMost(16f)
}

/**
 * Determine if stop markers should be visible at current zoom level.
 * Hide markers when zoomed out too far to reduce clutter.
 */
private fun shouldShowStopMarkers(zoomLevel: Double): Boolean {
    return zoomLevel >= 14.0  // Hide markers below zoom level 14
}

@Composable
private fun PlayerMap(state: RoutePlayerUiState, isFreeCam: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCenteredOnce by remember { mutableStateOf(false) }
    val mapView = remember { newOsmMapView(context) }
    var currentZoomLevel by remember { mutableStateOf(18.0) }
    var manualScaleOverride by remember { mutableStateOf<Float?>(null) }
    var autoScaleFactor by remember { mutableStateOf(1f) }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    // Show scale control slider when zoomed out enough
    val showScaleControl = currentZoomLevel < 14.0

    Box(modifier = modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView }) { view ->
            // Track zoom level for scaling
            currentZoomLevel = view.zoomLevelDouble
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
            // Calculate scale factor - use manual override if set, otherwise auto-calculate
            autoScaleFactor = calculateScaleFactor(currentZoomLevel)
            val scaleFactor = manualScaleOverride ?: autoScaleFactor

            // Base line width - reduced from 20f to be thinner at full scale
            val baseLineWidth = 10f
            // Scale line width but cap at reasonable sizes
            val scaledLineWidth = when {
                scaleFactor <= 1f -> (baseLineWidth * scaleFactor).coerceAtMost(15f)  // At full zoom or closer
                else -> (baseLineWidth * scaleFactor).coerceAtMost(50f)  // When zoomed out
            }

            // Offset polyline to the right and generate U-turn arcs
            // Pass line width so offset distance equals line thickness
            val offsetResult = offsetPolylineRight(routeLine, scaleFactor, scaledLineWidth)

            // Draw offset segments (right side of road)
            offsetResult.offsetSegments.forEach { segment ->
                if (segment.size >= 2) {
                    view.overlays.add(
                        Polyline(view).apply {
                            setPoints(segment.map { OsmGeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = routeLineColor
                            outlinePaint.strokeWidth = scaledLineWidth
                            // Add arrow pattern to show direction
                            outlinePaint.style = Paint.Style.STROKE
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                        }
                    )

                    // Add arrow markers along the segment
                    addArrowMarkers(view, segment, scaleFactor)
                }
            }

            // Draw U-turn arcs
            offsetResult.uturnArcs.forEach { arc ->
                if (arc.size >= 2) {
                    view.overlays.add(
                        Polyline(view).apply {
                            setPoints(arc.map { OsmGeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = routeLineColor
                            outlinePaint.strokeWidth = scaledLineWidth
                            outlinePaint.style = Paint.Style.STROKE
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                        }
                    )
                }
            }
        }

        // Only show stop markers when zoomed in enough (declutter when zoomed out for printing)
        if (shouldShowStopMarkers(currentZoomLevel)) {
            state.stops.forEachIndexed { index, stop ->
                view.overlays.add(
                    Marker(view).apply {
                        position = OsmGeoPoint(stop.latitude, stop.longitude)
                        title = "${index + 1}. ${stop.addressLabel}"
                    }
                )
            }
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

        // Manual scale control slider (shown when zoomed out)
        if (showScaleControl) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Polyline Scale",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Auto", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = manualScaleOverride ?: autoScaleFactor,
                            onValueChange = { manualScaleOverride = it },
                            valueRange = 1f..100f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(manualScaleOverride ?: autoScaleFactor).toInt()}x", style = MaterialTheme.typography.bodySmall)
                    }
                    if (manualScaleOverride != null) {
                        TextButton(
                            onClick = { manualScaleOverride = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Reset to Auto")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add arrow markers along a polyline segment to show direction of travel.
 * Places arrows at regular intervals along the segment.
 */
private fun addArrowMarkers(mapView: org.osmdroid.views.MapView, segment: List<GeoPoint>, scaleFactor: Float) {
    if (segment.size < 2) return

    // Calculate total path length
    var totalDistance = 0.0
    val segmentDistances = mutableListOf<Double>()
    for (i in 1 until segment.size) {
        val dist = distanceMeters(segment[i - 1], segment[i])
        segmentDistances.add(dist)
        totalDistance += dist
    }

    // Place arrows at scaled intervals (wider spacing when zoomed out)
    // Use enhanced scaling to match the arrow size growth
    val baseArrowInterval = 50.0
    val enhancedScale = if (scaleFactor > 1f) scaleFactor * scaleFactor else scaleFactor
    val scaledArrowInterval = baseArrowInterval * enhancedScale
    val numArrows = (totalDistance / scaledArrowInterval).toInt().coerceAtLeast(1)

    for (arrowIdx in 1..numArrows) {
        val targetDistance = arrowIdx * scaledArrowInterval
        var accumulated = 0.0
        var segmentIdx = 0

        // Find which segment this arrow should be on
        while (segmentIdx < segmentDistances.size && accumulated + segmentDistances[segmentIdx] < targetDistance) {
            accumulated += segmentDistances[segmentIdx]
            segmentIdx++
        }

        if (segmentIdx >= segmentDistances.size) break

        // Interpolate position within the segment
        val remainingDist = targetDistance - accumulated
        val segmentDist = segmentDistances[segmentIdx]
        val t = if (segmentDist > 0) remainingDist / segmentDist else 0.0

        val p1 = segment[segmentIdx]
        val p2 = segment[segmentIdx + 1]

        val arrowLat = p1.latitude + (p2.latitude - p1.latitude) * t
        val arrowLon = p1.longitude + (p2.longitude - p1.longitude) * t

        // Create a scaled arrow marker
        mapView.overlays.add(
            Marker(mapView).apply {
                position = OsmGeoPoint(arrowLat, arrowLon)
                // Use a simple arrow character or icon
                icon = createArrowIcon(scaleFactor)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                // Calculate rotation based on bearing
                val bearing = calculateBearing(p1, p2)
                rotation = bearing.toFloat()
            }
        )
    }
}

/**
 * Calculate bearing from point A to point B in degrees.
 */
private fun calculateBearing(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)

    val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
    val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
            kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)

    val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    return (bearing + 360.0) % 360.0
}

/**
 * Create a simple arrow icon for direction markers with scaling.
 * Arrows scale more aggressively than polylines to stay visible when zoomed out.
 */
private fun createArrowIcon(scaleFactor: Float): android.graphics.drawable.Drawable {
    val baseSize = 24f
    // Apply square scaling to make arrows grow faster than polylines when zoomed out
    val enhancedScale = if (scaleFactor > 1f) {
        scaleFactor * scaleFactor
    } else {
        scaleFactor
    }
    val scaledSize = (baseSize * enhancedScale).toInt().coerceAtLeast(24).coerceAtMost(200)
    val half = scaledSize / 2f
    val notchY = scaledSize * 0.75f

    return android.graphics.drawable.ShapeDrawable().apply {
        intrinsicWidth = scaledSize
        intrinsicHeight = scaledSize
        paint.color = routeLineColor
        paint.style = Paint.Style.FILL

        // Create arrow shape
        val path = Path()
        path.moveTo(half, 0f)  // Top point
        path.lineTo(0f, scaledSize.toFloat())  // Bottom left
        path.lineTo(half, notchY) // Middle notch
        path.lineTo(scaledSize.toFloat(), scaledSize.toFloat()) // Bottom right
        path.close()

        shape = object : android.graphics.drawable.shapes.PathShape(path, scaledSize.toFloat(), scaledSize.toFloat()) {}
    }
}

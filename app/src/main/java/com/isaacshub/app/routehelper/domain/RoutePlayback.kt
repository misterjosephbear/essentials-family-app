package com.isaacshub.app.routehelper.domain

/** How close (in meters) the driver must get to a stop before the player advances to it (10 feet ≈ 3 meters). */
const val STOP_ARRIVAL_RADIUS_METERS = 3.0

/**
 * If the driver is currently within [STOP_ARRIVAL_RADIUS_METERS] of ANY remaining stop, fast-forwards to
 * that stop (or the earliest one in sequence if multiple are nearby). Otherwise stays at the current index.
 * Never advances past the end of [stops] - once every stop has been reached, the index holds at `stops.size`.
 */
fun advanceToNextStop(currentLocation: GeoPoint, stops: List<GeoPoint>, currentIndex: Int): Int {
    val index = currentIndex.coerceIn(0, stops.size)
    if (index >= stops.size) return index

    // Check all remaining stops - if driver is near any of them, jump to the earliest one in sequence
    for (i in index until stops.size) {
        if (distanceMeters(currentLocation, stops[i]) < STOP_ARRIVAL_RADIUS_METERS) {
            return i + 1  // Advance to this stop (return i+1 to mark it as passed)
        }
    }

    return index  // No nearby stops, stay at current position
}

/** Below this speed a fresh GPS bearing reading is too noisy to trust, so the map keeps its last known heading. */
private const val MIN_BEARING_SPEED_MPS = 1.0f

/**
 * The heading the route player's map should visually point "up" toward: the driver's live GPS bearing
 * while moving fast enough for that reading to be reliable, otherwise whatever it was already showing.
 */
fun resolveMapBearing(sample: LocationSample, previousBearingDegrees: Float): Float {
    val bearing = sample.bearingDegrees
    return if (bearing != null && sample.speedMetersPerSecond >= MIN_BEARING_SPEED_MPS) bearing else previousBearingDegrees
}

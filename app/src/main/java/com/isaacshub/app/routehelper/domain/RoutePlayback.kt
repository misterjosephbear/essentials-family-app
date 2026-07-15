package com.isaacshub.app.routehelper.domain

/** How close (in meters) the driver must get to a stop before the player advances to the next one. */
const val STOP_ARRIVAL_RADIUS_METERS = 30.0

/**
 * If the driver is currently within [STOP_ARRIVAL_RADIUS_METERS] of `stops[currentIndex]`, advances to
 * the next stop; otherwise stays put. Never advances past the end of [stops] - once every stop has been
 * reached (or the route has none), the index just holds at `stops.size`.
 */
fun advanceToNextStop(currentLocation: GeoPoint, stops: List<GeoPoint>, currentIndex: Int): Int {
    val index = currentIndex.coerceIn(0, stops.size)
    if (index >= stops.size) return index
    return if (distanceMeters(currentLocation, stops[index]) < STOP_ARRIVAL_RADIUS_METERS) index + 1 else index
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

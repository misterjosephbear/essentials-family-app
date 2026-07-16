package com.isaacshub.app.routehelper.domain

/** How close (in meters) the driver must get to a stop before the player advances to it (10 feet ≈ 3 meters). */
const val STOP_ARRIVAL_RADIUS_METERS = 3.0

/**
 * How far behind the driver a stop can be before it's considered "passed" and auto-skipped.
 * This allows skipping stops when driving past them without stopping (30 feet ≈ 9 meters).
 */
const val STOP_PASSED_RADIUS_METERS = 9.0

/**
 * Advances to the next stop based on the driver's current location and previous location.
 *
 * Rules:
 * 1. If within STOP_ARRIVAL_RADIUS of any remaining stop, jump to that stop (or earliest if multiple)
 * 2. If moving and the next stop is now STOP_PASSED_RADIUS behind us, skip it (passed without stopping)
 * 3. Otherwise stay at current index
 *
 * Never advances past the end of [stops] - once every stop has been reached, index holds at `stops.size`.
 */
fun advanceToNextStop(
    currentLocation: GeoPoint,
    previousLocation: GeoPoint?,
    stops: List<GeoPoint>,
    currentIndex: Int
): Int {
    val index = currentIndex.coerceIn(0, stops.size)
    if (index >= stops.size) return index

    // Check all remaining stops - if driver is near any of them, jump to the earliest one in sequence
    for (i in index until stops.size) {
        if (distanceMeters(currentLocation, stops[i]) < STOP_ARRIVAL_RADIUS_METERS) {
            return i + 1  // Advance to this stop (return i+1 to mark it as passed)
        }
    }

    // If we have a previous location and we're moving, check if we passed the next stop
    if (previousLocation != null && index < stops.size) {
        val nextStop = stops[index]
        val distanceToPrev = distanceMeters(previousLocation, nextStop)
        val distanceToCurrent = distanceMeters(currentLocation, nextStop)

        // If the stop is now behind us (distance increasing) and far enough behind, skip it
        if (distanceToCurrent > distanceToPrev && distanceToCurrent > STOP_PASSED_RADIUS_METERS) {
            return index + 1  // Skip this stop, advance to next
        }
    }

    return index  // No nearby stops and didn't pass any, stay at current position
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

package com.isaacshub.app.routehelper.domain

/** How close (in meters) the driver must get to a stop before the player advances to it (10 feet ≈ 3 meters). */
const val STOP_ARRIVAL_RADIUS_METERS = 3.0

/**
 * How far behind the driver a stop can be before it's considered "passed" and auto-skipped.
 * This allows skipping stops when driving past them without stopping (30 feet ≈ 9 meters).
 */
const val STOP_PASSED_RADIUS_METERS = 9.0

/** Minimum speed (m/s) required to trigger auto-skip (about 2 mph). Prevents skipping when stationary. */
private const val MIN_SKIP_SPEED_MPS = 1.0f

/**
 * Advances to the next stop based on the driver's current location and previous location.
 *
 * Rules:
 * 1. If within STOP_ARRIVAL_RADIUS of any remaining stop, stay at that stop until moving away
 * 2. If currently at a stop and moving away from it, advance to next stop
 * 3. If moving fast enough and the next stop is now STOP_PASSED_RADIUS behind us, skip it (passed without stopping)
 * 4. Otherwise stay at current index
 *
 * Never advances past the end of [stops] - once every stop has been reached, index holds at `stops.size`.
 */
fun advanceToNextStop(
    currentLocation: GeoPoint,
    previousLocation: GeoPoint?,
    stops: List<GeoPoint>,
    currentIndex: Int,
    speedMetersPerSecond: Float
): Int {
    val index = currentIndex.coerceIn(0, stops.size)
    if (index >= stops.size) return index

    // Check if still at current stop
    if (index < stops.size) {
        val currentStop = stops[index]
        val distanceToCurrentStop = distanceMeters(currentLocation, currentStop)

        // If within arrival radius of current stop, stay there
        if (distanceToCurrentStop < STOP_ARRIVAL_RADIUS_METERS) {
            return index  // Stay at current stop until moving away
        }

        // If we were at this stop and are now moving away from it, advance to next stop
        if (previousLocation != null && speedMetersPerSecond >= MIN_SKIP_SPEED_MPS) {
            val prevDistanceToCurrentStop = distanceMeters(previousLocation, currentStop)
            // If we were within arrival radius and now moving away, advance
            if (prevDistanceToCurrentStop < STOP_ARRIVAL_RADIUS_METERS &&
                distanceToCurrentStop > prevDistanceToCurrentStop) {
                return index + 1  // Driver is leaving current stop, advance to next
            }
        }
    }

    // Check all remaining stops ahead - if driver is near any of them, jump to that stop
    // BUT: Skip stops that are very close to the current stop (same cluster)
    // to avoid jumping back to nearby boxes when leaving a cluster
    val CLUSTER_RADIUS_METERS = 15.0
    for (i in (index + 1) until stops.size) {
        val distanceFromCurrentStop = if (index < stops.size) {
            distanceMeters(stops[index], stops[i])
        } else {
            Double.MAX_VALUE
        }

        // Only jump to this stop if it's far from the current stop (not in same cluster)
        // OR if we're actually within arrival radius of it
        val isInSameCluster = distanceFromCurrentStop < CLUSTER_RADIUS_METERS
        val isNearThisStop = distanceMeters(currentLocation, stops[i]) < STOP_ARRIVAL_RADIUS_METERS

        if (isNearThisStop && !isInSameCluster) {
            return i  // Jump to this stop (it's far enough away to be a different cluster)
        }
    }

    // Only auto-skip if moving at reasonable speed (prevents GPS drift from skipping while stopped)
    if (previousLocation != null && index < stops.size && speedMetersPerSecond >= MIN_SKIP_SPEED_MPS) {
        val nextStop = stops[index]
        val distanceToPrev = distanceMeters(previousLocation, nextStop)
        val distanceToCurrent = distanceMeters(currentLocation, nextStop)

        // Must be moving away from stop (distance increasing) AND far enough behind
        // AND the previous location was closer than the pass radius (ensures we actually approached it)
        if (distanceToCurrent > distanceToPrev &&
            distanceToCurrent > STOP_PASSED_RADIUS_METERS &&
            distanceToPrev < STOP_PASSED_RADIUS_METERS) {
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

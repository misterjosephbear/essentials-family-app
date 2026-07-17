package com.isaacshub.app.routehelper.domain

import kotlin.math.*

/**
 * Utilities for offsetting polylines to the right of the road based on direction of travel,
 * and generating U-turn arcs at turnaround points.
 */

/** Offset distance from center line in meters (right side of road). */
private const val OFFSET_DISTANCE_METERS = 4.0

/** Radius for U-turn arcs in meters. */
private const val UTURN_ARC_RADIUS_METERS = 6.0

/** Number of points to use when generating a U-turn arc. */
private const val UTURN_ARC_POINTS = 12

/** Minimum bearing change to consider a point a turnaround (degrees). */
private const val TURNAROUND_THRESHOLD_DEGREES = 90.0

/**
 * Result of offsetting a polyline: contains the offset segments and turnaround arc segments.
 */
data class OffsetPolylineResult(
    /** Offset polyline segments (right side of road). */
    val offsetSegments: List<List<GeoPoint>>,
    /** U-turn arc segments connecting turnarounds. */
    val uturnArcs: List<List<GeoPoint>>
)

/**
 * Offset a polyline to the right of the road based on direction of travel.
 * Detects turnarounds and creates U-turn arcs to connect them.
 *
 * @param points The original polyline points (center of road).
 * @param scaleFactor Scaling factor for offset distance (1.0 = normal, >1.0 = wider offset for zoomed out views).
 * @return OffsetPolylineResult containing offset segments and U-turn arcs.
 */
fun offsetPolylineRight(points: List<GeoPoint>, scaleFactor: Float = 1.0f): OffsetPolylineResult {
    if (points.size < 2) {
        return OffsetPolylineResult(emptyList(), emptyList())
    }

    // Detect turnaround indices
    val turnaroundIndices = detectTurnaroundIndices(points)

    // Split polyline at turnarounds
    val segments = splitAtTurnarounds(points, turnaroundIndices)

    // Calculate scaled offset distance
    val scaledOffsetDistance = OFFSET_DISTANCE_METERS * scaleFactor
    val scaledArcRadius = UTURN_ARC_RADIUS_METERS * scaleFactor

    // Offset each segment to the right
    val offsetSegments = segments.map { segment ->
        offsetSegmentRight(segment, scaledOffsetDistance)
    }

    // Generate U-turn arcs at turnaround points
    val uturnArcs = mutableListOf<List<GeoPoint>>()
    for (i in turnaroundIndices) {
        if (i > 0 && i < points.size - 1) {
            val arc = generateUturnArc(
                turnaroundPoint = points[i],
                fromPoint = points[i - 1],
                toPoint = points[i + 1],
                offsetDistance = scaledOffsetDistance,
                arcRadius = scaledArcRadius
            )
            uturnArcs.add(arc)
        }
    }

    return OffsetPolylineResult(offsetSegments, uturnArcs)
}

/**
 * Detect indices where bearing changes significantly (turnarounds).
 */
private fun detectTurnaroundIndices(points: List<GeoPoint>): List<Int> {
    if (points.size < 3) return emptyList()

    val turnarounds = mutableListOf<Int>()

    for (i in 1 until points.size - 1) {
        val prev = points[i - 1]
        val current = points[i]
        val next = points[i + 1]

        // Skip if points are too close together (duplicates)
        if (distanceMeters(current, prev) < 1.0 || distanceMeters(current, next) < 1.0) {
            continue
        }

        val bearingIn = calculateBearing(prev, current)
        val bearingOut = calculateBearing(current, next)
        val bearingDiff = normalizeBearingDiff(bearingOut - bearingIn)

        if (bearingDiff > TURNAROUND_THRESHOLD_DEGREES) {
            turnarounds.add(i)
        }
    }

    return turnarounds
}

/**
 * Split polyline into segments at turnaround points.
 */
private fun splitAtTurnarounds(points: List<GeoPoint>, turnaroundIndices: List<Int>): List<List<GeoPoint>> {
    if (turnaroundIndices.isEmpty()) {
        return listOf(points)
    }

    val segments = mutableListOf<List<GeoPoint>>()
    var startIdx = 0

    for (turnaroundIdx in turnaroundIndices.sorted()) {
        if (turnaroundIdx > startIdx) {
            // Segment up to (and including) the turnaround point
            segments.add(points.subList(startIdx, turnaroundIdx + 1))
            startIdx = turnaroundIdx
        }
    }

    // Add final segment after last turnaround
    if (startIdx < points.size - 1) {
        segments.add(points.subList(startIdx, points.size))
    }

    return segments
}

/**
 * Offset a single segment to the right based on direction of travel.
 */
private fun offsetSegmentRight(segment: List<GeoPoint>, offsetDistance: Double): List<GeoPoint> {
    if (segment.size < 2) return segment

    val offsetPoints = mutableListOf<GeoPoint>()

    for (i in segment.indices) {
        val point = segment[i]

        // Calculate bearing for offset direction
        val bearing = when {
            i == 0 -> {
                // First point: use bearing to next point
                calculateBearing(segment[0], segment[1])
            }
            i == segment.size - 1 -> {
                // Last point: use bearing from previous point
                calculateBearing(segment[i - 1], segment[i])
            }
            else -> {
                // Middle point: average bearing from previous and to next
                val bearingIn = calculateBearing(segment[i - 1], segment[i])
                val bearingOut = calculateBearing(segment[i], segment[i + 1])
                averageBearing(bearingIn, bearingOut)
            }
        }

        // Offset to the right (bearing + 90 degrees)
        val offsetBearing = (bearing + 90.0) % 360.0
        val offsetPoint = offsetPoint(point, offsetBearing, offsetDistance)
        offsetPoints.add(offsetPoint)
    }

    return offsetPoints
}

/**
 * Generate a U-turn arc connecting two segments at a turnaround point.
 */
private fun generateUturnArc(
    turnaroundPoint: GeoPoint,
    fromPoint: GeoPoint,
    toPoint: GeoPoint,
    offsetDistance: Double,
    arcRadius: Double
): List<GeoPoint> {
    val bearingIn = calculateBearing(fromPoint, turnaroundPoint)
    val bearingOut = calculateBearing(turnaroundPoint, toPoint)

    // Calculate the center of the U-turn arc
    // The arc goes from the end of the incoming offset segment to the start of the outgoing offset segment
    val offsetBearingIn = (bearingIn + 90.0) % 360.0  // Right offset of incoming
    val offsetBearingOut = (bearingOut + 90.0) % 360.0  // Right offset of outgoing

    // Start and end points of the arc (offset from turnaround point)
    val arcStart = offsetPoint(turnaroundPoint, offsetBearingIn, offsetDistance)
    val arcEnd = offsetPoint(turnaroundPoint, offsetBearingOut, offsetDistance)

    // Generate arc points between start and end
    val arcPoints = mutableListOf<GeoPoint>()
    arcPoints.add(arcStart)

    // For U-turns, we want the arc to go OUTWARD (the long way around)
    // Calculate the outer arc bearing by going the opposite direction
    val bearingDiff = normalizeBearingDiff(offsetBearingOut - offsetBearingIn)
    val goLongWay = bearingDiff < 180.0  // If short way is < 180, we need to go the long way

    // Generate intermediate arc points
    for (i in 1 until UTURN_ARC_POINTS) {
        val t = i.toDouble() / UTURN_ARC_POINTS

        // Interpolate bearing going the LONG way (outward arc for U-turn)
        val bearing = if (goLongWay) {
            // Go the opposite direction (add 360 to force long way)
            val start = offsetBearingIn
            val end = if (offsetBearingOut > offsetBearingIn) offsetBearingOut - 360.0 else offsetBearingOut
            ((start + (end - start) * t) + 360.0) % 360.0
        } else {
            // Already going long way
            interpolateBearing(offsetBearingIn, offsetBearingOut, t)
        }

        // Create wider arc by increasing distance at the midpoint
        val distance = offsetDistance + arcRadius * sin(t * PI)
        val arcPoint = offsetPoint(turnaroundPoint, bearing, distance)
        arcPoints.add(arcPoint)
    }

    arcPoints.add(arcEnd)
    return arcPoints
}

/**
 * Calculate bearing from point A to point B in degrees (0-360).
 */
private fun calculateBearing(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    val bearing = Math.toDegrees(atan2(y, x))
    return (bearing + 360.0) % 360.0
}

/**
 * Normalize bearing difference to [0, 180] range.
 */
private fun normalizeBearingDiff(diff: Double): Double {
    val normalized = ((diff % 360.0) + 360.0) % 360.0
    return if (normalized > 180.0) 360.0 - normalized else normalized
}

/**
 * Average two bearings, handling wraparound correctly.
 */
private fun averageBearing(bearing1: Double, bearing2: Double): Double {
    val diff = normalizeBearingDiff(bearing2 - bearing1)
    val result = if (diff > 180.0) {
        // Shorter path goes the other way
        (bearing1 - (360.0 - diff) / 2.0 + 360.0) % 360.0
    } else {
        (bearing1 + diff / 2.0) % 360.0
    }
    return result
}

/**
 * Interpolate between two bearings with parameter t in [0, 1].
 */
private fun interpolateBearing(bearing1: Double, bearing2: Double, t: Double): Double {
    val diff = normalizeBearingDiff(bearing2 - bearing1)
    val shortest = if (diff > 180.0) {
        // Go the other way
        bearing1 - (360.0 - diff) * t
    } else {
        bearing1 + diff * t
    }
    return (shortest + 360.0) % 360.0
}

/**
 * Offset a point by a distance in a specific bearing direction.
 */
private fun offsetPoint(point: GeoPoint, bearingDegrees: Double, distanceMeters: Double): GeoPoint {
    val earthRadiusMeters = 6371000.0
    val bearingRad = Math.toRadians(bearingDegrees)
    val latRad = Math.toRadians(point.latitude)
    val lonRad = Math.toRadians(point.longitude)
    val angularDistance = distanceMeters / earthRadiusMeters

    val newLatRad = asin(
        sin(latRad) * cos(angularDistance) +
        cos(latRad) * sin(angularDistance) * cos(bearingRad)
    )

    val newLonRad = lonRad + atan2(
        sin(bearingRad) * sin(angularDistance) * cos(latRad),
        cos(angularDistance) - sin(latRad) * sin(newLatRad)
    )

    return GeoPoint(
        latitude = Math.toDegrees(newLatRad),
        longitude = Math.toDegrees(newLonRad)
    )
}

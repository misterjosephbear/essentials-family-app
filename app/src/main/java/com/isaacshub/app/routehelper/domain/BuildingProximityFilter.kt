package com.isaacshub.app.routehelper.domain

import kotlin.math.floor

/** Floor for a segment's accept threshold - roughly an in-town lot's setback from the road. */
private const val BASE_THRESHOLD_METERS = 40.0

/**
 * How much slack beyond a segment's own closest real-building distance to allow for its other
 * candidates. If even the nearest candidate on a segment sits 90m from any building, real houses
 * there likely sit ~90m back from the road, so its other candidates are checked against a
 * proportionally wider band instead of [BASE_THRESHOLD_METERS].
 */
private const val SLACK_FACTOR = 2.5

/** ~111m latitude x ~85m longitude at mid US latitudes. */
private const val GRID_CELL_DEGREES = 0.001

/** Cells searched outward in each direction - wide enough to find real rural setback distances (100m+), not just in-town ones. */
private const val GRID_SEARCH_RADIUS_CELLS = 4

private fun cellIndex(value: Double): Long = floor(value / GRID_CELL_DEGREES).toLong()
private fun cellKey(latCell: Long, lonCell: Long): Long = (latCell shl 32) xor (lonCell and 0xFFFFFFFFL)

private class BuildingGrid(buildings: List<GeoPoint>) {
    private val cells: Map<Long, List<GeoPoint>> =
        buildings.groupBy { cellKey(cellIndex(it.latitude), cellIndex(it.longitude)) }

    /** Distance to the nearest building within the search radius, or null if none is found that close. */
    fun nearestDistance(point: GeoPoint): Double? {
        val latCell = cellIndex(point.latitude)
        val lonCell = cellIndex(point.longitude)
        var best: Double? = null
        for (dLat in -GRID_SEARCH_RADIUS_CELLS..GRID_SEARCH_RADIUS_CELLS) {
            for (dLon in -GRID_SEARCH_RADIUS_CELLS..GRID_SEARCH_RADIUS_CELLS) {
                val bucket = cells[cellKey(latCell + dLat, lonCell + dLon)] ?: continue
                for (building in bucket) {
                    val distance = distanceMeters(point, building)
                    if (best == null || distance < best!!) best = distance
                }
            }
        }
        return best
    }
}

/**
 * Keeps only the addresses within a real building's reach, checked per street-segment-side group
 * rather than against one fixed distance for the whole ZIP. TIGER's house-number ranges only
 * describe a numbering scheme, not which numbers have an actual structure, so interpolated points
 * can land on numbers with no real house - but how far back a real house sits from the road varies
 * hugely between a dense in-town block and a rural road, and a single global threshold either lets
 * every rural candidate through unfiltered (too loose) or drops every rural candidate outright (too
 * tight, and what caused a whole rural stretch of a real route to show zero addresses). If a group
 * has no building anywhere within the search radius, it's left unfiltered entirely - there's no
 * reliable signal to filter by, so trusting TIGER's range beats dropping every candidate on it.
 */
fun filterAddressGroupsNearBuildings(
    addressGroups: List<List<InterpolatedAddress>>,
    buildings: List<GeoPoint>
): List<InterpolatedAddress> {
    if (buildings.isEmpty()) return addressGroups.flatten()
    val grid = BuildingGrid(buildings)

    return addressGroups.flatMap { group ->
        val distances = group.associateWith { grid.nearestDistance(it.location) }
        val minDistance = distances.values.filterNotNull().minOrNull() ?: return@flatMap group
        val threshold = maxOf(BASE_THRESHOLD_METERS, minDistance * SLACK_FACTOR)
        group.filter { (distances[it] ?: Double.MAX_VALUE) <= threshold }
    }
}

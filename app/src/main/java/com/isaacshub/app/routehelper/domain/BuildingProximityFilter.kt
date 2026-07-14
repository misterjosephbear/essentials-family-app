package com.isaacshub.app.routehelper.domain

import kotlin.math.floor

private const val NEARBY_BUILDING_THRESHOLD_METERS = 40.0

/**
 * Grid cell size in degrees for bucketing buildings before a proximity check. ~111m latitude x ~85m
 * longitude at mid US latitudes - comfortably larger than [NEARBY_BUILDING_THRESHOLD_METERS], so
 * checking a candidate's own cell plus its 8 neighbors can never miss a real match, while avoiding a
 * naive O(addresses x buildings) scan: a tile can hold ~70k buildings, and checking every one of them
 * against every candidate (each a haversine call) is what made this filter take over a minute before
 * this index was added.
 */
private const val GRID_CELL_DEGREES = 0.001

private fun cellKey(point: GeoPoint): Long {
    val latCell = floor(point.latitude / GRID_CELL_DEGREES).toLong()
    val lonCell = floor(point.longitude / GRID_CELL_DEGREES).toLong()
    return (latCell shl 32) xor (lonCell and 0xFFFFFFFFL)
}

/**
 * Keeps only the [addresses] within [thresholdMeters] of a real building footprint - TIGER's
 * house-number ranges only describe a numbering scheme, not which numbers have an actual structure,
 * so interpolated points can land on numbers with no real house. Cross-referencing against Microsoft's
 * building footprints data drops those. If [buildings] is empty (the footprint fetch failed or found
 * nothing), addresses are returned unfiltered - a failed best-effort check should never remove every
 * candidate.
 */
fun filterAddressesNearBuildings(
    addresses: List<InterpolatedAddress>,
    buildings: List<GeoPoint>,
    thresholdMeters: Double = NEARBY_BUILDING_THRESHOLD_METERS
): List<InterpolatedAddress> {
    if (buildings.isEmpty()) return addresses

    val grid = HashMap<Long, MutableList<GeoPoint>>()
    for (building in buildings) {
        grid.getOrPut(cellKey(building)) { mutableListOf() }.add(building)
    }

    return addresses.filter { address -> hasNearbyBuilding(address.location, grid, thresholdMeters) }
}

private fun hasNearbyBuilding(location: GeoPoint, grid: Map<Long, List<GeoPoint>>, thresholdMeters: Double): Boolean {
    val latCell = floor(location.latitude / GRID_CELL_DEGREES).toLong()
    val lonCell = floor(location.longitude / GRID_CELL_DEGREES).toLong()
    for (dLat in -1..1) {
        for (dLon in -1..1) {
            val key = ((latCell + dLat) shl 32) xor ((lonCell + dLon) and 0xFFFFFFFFL)
            val bucket = grid[key] ?: continue
            if (bucket.any { distanceMeters(location, it) <= thresholdMeters }) return true
        }
    }
    return false
}

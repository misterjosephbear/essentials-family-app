package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingProximityFilterTest {

    @Test
    fun `keeps addresses close to a building and drops distant ones within the same tight in-town group`() {
        val building = GeoPoint(39.30000, -85.70000)
        val close = InterpolatedAddress("100 Main St", GeoPoint(39.30003, -85.70000)) // ~3m away
        val far = InterpolatedAddress("200 Main St", GeoPoint(39.31000, -85.70000)) // ~1.1km away

        val result = filterAddressGroupsNearBuildings(listOf(listOf(close, far)), listOf(building))

        assertEquals(listOf(close), result)
    }

    @Test
    fun `returns everything unfiltered when there are no buildings to check against`() {
        val addresses = listOf(InterpolatedAddress("100 Main St", GeoPoint(39.30003, -85.70000)))
        assertEquals(addresses, filterAddressGroupsNearBuildings(listOf(addresses), emptyList()))
    }

    @Test
    fun `keeps a rural group whose real houses sit well back from the road`() {
        // Regression test: a real rural segment (E Jackson Rd) had every candidate 60-120m from the
        // nearest building - a flat 40m threshold dropped the whole segment, when a global one would
        // have needed the tighter in-town threshold to stay meaningful elsewhere.
        val building = GeoPoint(39.30000, -85.70000)
        val ruralGroup = listOf(
            InterpolatedAddress("15100 E Jackson Rd", offsetMeters(building, 63.0)),
            InterpolatedAddress("15200 E Jackson Rd", offsetMeters(building, 80.0)),
            InterpolatedAddress("15300 E Jackson Rd", offsetMeters(building, 122.0))
        )

        val result = filterAddressGroupsNearBuildings(listOf(ruralGroup), listOf(building))

        assertEquals(ruralGroup, result)
    }

    @Test
    fun `still drops an outlier far beyond a rural group's own nearest-building distance`() {
        val building = GeoPoint(39.30000, -85.70000)
        val near = InterpolatedAddress("15100 E Jackson Rd", offsetMeters(building, 63.0))
        val wayOff = InterpolatedAddress("15900 E Jackson Rd", offsetMeters(building, 2000.0))

        val result = filterAddressGroupsNearBuildings(listOf(listOf(near, wayOff)), listOf(building))

        assertEquals(listOf(near), result)
    }

    @Test
    fun `leaves a group unfiltered when no building is found anywhere near it`() {
        val farAwayBuilding = GeoPoint(40.0, -86.0)
        val group = listOf(InterpolatedAddress("100 Remote Rd", GeoPoint(39.30000, -85.70000)))

        val result = filterAddressGroupsNearBuildings(listOf(group), listOf(farAwayBuilding))

        assertEquals(group, result)
    }

    @Test
    fun `each group is calibrated independently`() {
        val townBuilding = GeoPoint(39.30000, -85.70000)
        val ruralBuilding = GeoPoint(39.40000, -85.80000)

        val townGroup = listOf(
            InterpolatedAddress("100 Main St", offsetMeters(townBuilding, 20.0)),
            InterpolatedAddress("102 Main St", offsetMeters(townBuilding, 90.0)) // real in-town gap, should drop
        )
        val ruralGroup = listOf(
            InterpolatedAddress("15100 E Jackson Rd", offsetMeters(ruralBuilding, 90.0)) // real rural setback, should keep
        )

        val result = filterAddressGroupsNearBuildings(listOf(townGroup, ruralGroup), listOf(townBuilding, ruralBuilding))

        assertTrue(result.any { it.label == "100 Main St" })
        assertTrue(result.none { it.label == "102 Main St" })
        assertTrue(result.any { it.label == "15100 E Jackson Rd" })
    }

    /** A point roughly [meters] north of [from] - close enough for these threshold-boundary tests. */
    private fun offsetMeters(from: GeoPoint, meters: Double): GeoPoint {
        val metersPerDegreeLat = 111_195.0
        return GeoPoint(from.latitude + meters / metersPerDegreeLat, from.longitude)
    }
}

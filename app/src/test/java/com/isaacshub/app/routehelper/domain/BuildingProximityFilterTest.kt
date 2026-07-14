package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildingProximityFilterTest {

    private val nearBuilding = GeoPoint(39.30000, -85.70000)

    @Test
    fun `keeps addresses close to a building and drops distant ones`() {
        val close = InterpolatedAddress("100 Main St", GeoPoint(39.30003, -85.70000)) // ~3m away
        val far = InterpolatedAddress("200 Main St", GeoPoint(39.31000, -85.70000)) // ~1.1km away

        val result = filterAddressesNearBuildings(listOf(close, far), listOf(nearBuilding))

        assertEquals(listOf(close), result)
    }

    @Test
    fun `returns everything unfiltered when there are no buildings to check against`() {
        val addresses = listOf(InterpolatedAddress("100 Main St", GeoPoint(39.30003, -85.70000)))
        assertEquals(addresses, filterAddressesNearBuildings(addresses, emptyList()))
    }

    @Test
    fun `respects a custom threshold`() {
        val address = InterpolatedAddress("100 Main St", GeoPoint(39.3005, -85.70000)) // ~55m away
        assertEquals(emptyList<InterpolatedAddress>(), filterAddressesNearBuildings(listOf(address), listOf(nearBuilding), thresholdMeters = 20.0))
        assertEquals(listOf(address), filterAddressesNearBuildings(listOf(address), listOf(nearBuilding), thresholdMeters = 100.0))
    }
}

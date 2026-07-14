package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NearestAddressesTest {

    // Roughly 1 degree of latitude is ~111km, used to build points at known approximate distances.
    private val origin = GeoPoint(40.0, -80.0)

    @Test
    fun `distanceMeters is zero for the same point`() {
        assertEquals(0.0, distanceMeters(origin, origin), 0.001)
    }

    @Test
    fun `distanceMeters roughly matches known latitude degree distance`() {
        val oneDegreeNorth = GeoPoint(41.0, -80.0)
        val distance = distanceMeters(origin, oneDegreeNorth)
        // ~111,195m per degree of latitude - allow a reasonable margin.
        assertEquals(111_195.0, distance, 2000.0)
    }

    @Test
    fun `nearestAddresses returns closest-first up to count`() {
        val candidates = listOf(
            CandidateAddress(1, "Far", GeoPoint(41.0, -80.0)),
            CandidateAddress(2, "Closest", GeoPoint(40.001, -80.0)),
            CandidateAddress(3, "Middle", GeoPoint(40.01, -80.0))
        )
        val result = nearestAddresses(origin, candidates, count = 2)
        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    @Test
    fun `nearestAddresses returns fewer than count if there aren't enough candidates`() {
        val candidates = listOf(CandidateAddress(1, "Only", GeoPoint(40.001, -80.0)))
        val result = nearestAddresses(origin, candidates, count = 5)
        assertEquals(1, result.size)
    }

    @Test
    fun `nearestAddresses on an empty list returns empty`() {
        assertEquals(emptyList<CandidateAddress>(), nearestAddresses(origin, emptyList()))
    }
}

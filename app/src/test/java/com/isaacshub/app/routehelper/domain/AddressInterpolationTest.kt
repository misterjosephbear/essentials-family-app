package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressInterpolationTest {

    @Test
    fun `sampleHouseNumbers respects odd parity`() {
        val range = HouseNumberRange(from = 100, to = 110, parity = AddressParity.ODD)
        val result = sampleHouseNumbers(range, count = 5)
        assertTrue(result.all { it % 2 != 0 })
        assertTrue(result.all { it in 100..110 })
    }

    @Test
    fun `sampleHouseNumbers respects even parity`() {
        val range = HouseNumberRange(from = 101, to = 111, parity = AddressParity.EVEN)
        val result = sampleHouseNumbers(range, count = 5)
        assertTrue(result.all { it % 2 == 0 })
    }

    @Test
    fun `sampleHouseNumbers handles descending from-to`() {
        val range = HouseNumberRange(from = 200, to = 100, parity = AddressParity.BOTH)
        val result = sampleHouseNumbers(range, count = 3)
        assertTrue(result.all { it in 100..200 })
    }

    @Test
    fun `sampleHouseNumbers with a missing bound returns empty`() {
        assertEquals(emptyList<Int>(), sampleHouseNumbers(HouseNumberRange(null, 100, AddressParity.BOTH), 5))
        assertEquals(emptyList<Int>(), sampleHouseNumbers(HouseNumberRange(100, null, AddressParity.BOTH), 5))
    }

    @Test
    fun `sampleHouseNumbers with count 1 returns the midpoint`() {
        val result = sampleHouseNumbers(HouseNumberRange(100, 200, AddressParity.BOTH), 1)
        assertEquals(listOf(150), result)
    }

    @Test
    fun `pointsAlongLine spreads points from first to last vertex`() {
        val vertices = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))
        val points = pointsAlongLine(vertices, 3)
        assertEquals(3, points.size)
        assertEquals(0.0, points.first().longitude, 0.0001)
        assertEquals(1.0, points.last().longitude, 0.0001)
        assertEquals(0.5, points[1].longitude, 0.0001)
    }

    @Test
    fun `pointsAlongLine with a single vertex repeats it`() {
        val vertices = listOf(GeoPoint(1.0, 2.0))
        val points = pointsAlongLine(vertices, 4)
        assertEquals(4, points.size)
        assertTrue(points.all { it == GeoPoint(1.0, 2.0) })
    }

    @Test
    fun `pointsAlongLine with no vertices returns empty`() {
        assertEquals(emptyList<GeoPoint>(), pointsAlongLine(emptyList(), 5))
    }

    @Test
    fun `interpolateAddresses only includes the side matching the target zip`() {
        val feature = TigerAddressFeature(
            streetName = "Main St",
            leftRange = HouseNumberRange(100, 110, AddressParity.EVEN),
            rightRange = HouseNumberRange(101, 111, AddressParity.ODD),
            zipLeft = "47280",
            zipRight = "47201",
            vertices = listOf(GeoPoint(39.0, -85.0), GeoPoint(39.001, -85.0))
        )
        val result = interpolateAddresses(feature, "47280")
        assertTrue(result.isNotEmpty())
        val houseNumbers = result.map { it.label.substringBefore(" ").toInt() }
        assertTrue(houseNumbers.all { it % 2 == 0 })
    }

    @Test
    fun `interpolateAddresses caps points per side for a large range`() {
        val feature = TigerAddressFeature(
            streetName = "Rural Rd",
            leftRange = HouseNumberRange(10001, 11099, AddressParity.ODD),
            rightRange = HouseNumberRange(null, null, AddressParity.UNKNOWN),
            zipLeft = "47280",
            zipRight = "",
            vertices = listOf(GeoPoint(39.0, -85.0), GeoPoint(39.01, -85.01))
        )
        val result = interpolateAddresses(feature, "47280")
        assertTrue(result.isNotEmpty())
        assertTrue(result.size <= MAX_POINTS_PER_SIDE)
    }

    @Test
    fun `interpolateAddresses with no matching zip returns empty`() {
        val feature = TigerAddressFeature(
            streetName = "Elsewhere Ave",
            leftRange = HouseNumberRange(100, 110, AddressParity.BOTH),
            rightRange = HouseNumberRange(100, 110, AddressParity.BOTH),
            zipLeft = "00001",
            zipRight = "00002",
            vertices = listOf(GeoPoint(39.0, -85.0), GeoPoint(39.001, -85.0))
        )
        assertEquals(emptyList<InterpolatedAddress>(), interpolateAddresses(feature, "47280"))
    }

    @Test
    fun `parseParity maps known codes`() {
        assertEquals(AddressParity.ODD, parseParity("O"))
        assertEquals(AddressParity.EVEN, parseParity("e"))
        assertEquals(AddressParity.BOTH, parseParity("B"))
        assertEquals(AddressParity.UNKNOWN, parseParity(""))
    }
}

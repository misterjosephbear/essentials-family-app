package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutePlaybackTest {

    private val stopA = GeoPoint(40.0, -80.0)
    private val stopB = GeoPoint(40.001, -80.0) // roughly 111m north of stopA
    private val stops = listOf(stopA, stopB)

    @Test
    fun `stays put when far from the current target stop`() {
        val farAway = GeoPoint(41.0, -80.0)
        assertEquals(0, advanceToNextStop(farAway, stops, 0))
    }

    @Test
    fun `advances to the next stop once within arrival radius`() {
        assertEquals(1, advanceToNextStop(stopA, stops, 0))
    }

    @Test
    fun `does not advance past the last stop`() {
        assertEquals(2, advanceToNextStop(stopB, stops, 2))
    }

    @Test
    fun `an empty route never advances`() {
        assertEquals(0, advanceToNextStop(stopA, emptyList(), 0))
    }

    @Test
    fun `bearing updates while moving fast enough for a reliable reading`() {
        val sample = LocationSample(stopA, speedMetersPerSecond = 5f, bearingDegrees = 90f)
        assertEquals(90f, resolveMapBearing(sample, previousBearingDegrees = 0f))
    }

    @Test
    fun `bearing holds steady when moving too slowly to trust the reading`() {
        val sample = LocationSample(stopA, speedMetersPerSecond = 0.2f, bearingDegrees = 45f)
        assertEquals(0f, resolveMapBearing(sample, previousBearingDegrees = 0f))
    }

    @Test
    fun `bearing holds steady when the platform has no bearing reading yet`() {
        val sample = LocationSample(stopA, speedMetersPerSecond = 5f, bearingDegrees = null)
        assertEquals(120f, resolveMapBearing(sample, previousBearingDegrees = 120f))
    }
}

package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSampleTest {

    private val point = GeoPoint(40.0, -80.0)

    @Test
    fun `speed at or above the threshold is not stopped`() {
        assertFalse(LocationSample(point, STOPPED_SPEED_THRESHOLD_MPS).isStopped())
        assertFalse(LocationSample(point, 5f).isStopped())
    }

    @Test
    fun `speed below the threshold is stopped`() {
        assertTrue(LocationSample(point, 0f).isStopped())
        assertTrue(LocationSample(point, STOPPED_SPEED_THRESHOLD_MPS - 0.01f).isStopped())
    }

    @Test
    fun `a custom threshold is respected`() {
        assertTrue(LocationSample(point, 1f).isStopped(thresholdMetersPerSecond = 2f))
        assertFalse(LocationSample(point, 1f).isStopped(thresholdMetersPerSecond = 0.5f))
    }
}

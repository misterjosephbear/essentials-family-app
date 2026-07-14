package com.isaacshub.app.routehelper.domain

/** Below this speed the driver is considered fully stopped, not just slowing down for a mailbox. */
const val STOPPED_SPEED_THRESHOLD_MPS = 0.5f

/** A GPS fix paired with the device's reported speed, used to tell a rolling approach from a full stop. */
data class LocationSample(val point: GeoPoint, val speedMetersPerSecond: Float) {
    fun isStopped(thresholdMetersPerSecond: Float = STOPPED_SPEED_THRESHOLD_MPS): Boolean =
        speedMetersPerSecond < thresholdMetersPerSecond
}

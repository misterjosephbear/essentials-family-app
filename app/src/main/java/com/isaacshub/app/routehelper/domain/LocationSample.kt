package com.isaacshub.app.routehelper.domain

/** Below this speed the driver is considered fully stopped, not just slowing down for a mailbox. */
const val STOPPED_SPEED_THRESHOLD_MPS = 0.5f

/**
 * A GPS fix paired with the device's reported speed and heading, used to tell a rolling approach from a
 * full stop and to orient the route player's map. [bearingDegrees] is null whenever the platform hasn't
 * reported one yet (no fix, or too little movement for GPS to derive a heading).
 */
data class LocationSample(
    val point: GeoPoint,
    val speedMetersPerSecond: Float,
    val bearingDegrees: Float? = null
) {
    fun isStopped(thresholdMetersPerSecond: Float = STOPPED_SPEED_THRESHOLD_MPS): Boolean =
        speedMetersPerSecond < thresholdMetersPerSecond
}

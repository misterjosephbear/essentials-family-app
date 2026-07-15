package com.isaacshub.app.routehelper.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.LocationSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private fun Location.toSample(): LocationSample =
    LocationSample(
        GeoPoint(latitude, longitude),
        if (hasSpeed()) speed else 0f,
        if (hasBearing()) bearing else null
    )

/**
 * Live location as a Flow, using the plain platform LocationManager (GPS) rather than Google Play
 * Services' fused location provider - keeps this feature entirely off Google's stack, matching the
 * OSM-only approach for the map itself. Caller must have already checked ACCESS_FINE_LOCATION.
 *
 * [minDistanceMeters] defaults to 0 so fixes keep arriving on the time interval alone even while the
 * vehicle is stationary - callers that need to notice "the driver just came to a stop" (speed dropping
 * to ~0) require a fix at that moment, which a distance-gated request would never deliver.
 */
@SuppressLint("MissingPermission")
fun liveLocationFlow(
    context: Context,
    minIntervalMillis: Long = 1_000L,
    minDistanceMeters: Float = 0f
): Flow<LocationSample> = callbackFlow {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val listener = LocationListener { location: Location ->
        trySend(location.toSample())
    }

    val provider = when {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }

    if (provider == null) {
        close(IllegalStateException("No location provider is enabled on this device"))
    } else {
        locationManager.getLastKnownLocation(provider)?.let { location ->
            trySend(location.toSample())
        }
        locationManager.requestLocationUpdates(provider, minIntervalMillis, minDistanceMeters, listener, Looper.getMainLooper())
    }

    awaitClose { locationManager.removeUpdates(listener) }
}

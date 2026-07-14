package com.isaacshub.app.routehelper.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Live location as a Flow, using the plain platform LocationManager (GPS) rather than Google Play
 * Services' fused location provider - keeps this feature entirely off Google's stack, matching the
 * OSM-only approach for the map itself. Caller must have already checked ACCESS_FINE_LOCATION.
 */
@SuppressLint("MissingPermission")
fun liveLocationFlow(
    context: Context,
    minIntervalMillis: Long = 2_000L,
    minDistanceMeters: Float = 5f
): Flow<GeoPoint> = callbackFlow {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val listener = LocationListener { location: Location ->
        trySend(GeoPoint(location.latitude, location.longitude))
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
            trySend(GeoPoint(location.latitude, location.longitude))
        }
        locationManager.requestLocationUpdates(provider, minIntervalMillis, minDistanceMeters, listener, Looper.getMainLooper())
    }

    awaitClose { locationManager.removeUpdates(listener) }
}

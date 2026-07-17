package com.isaacshub.app.routehelper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_helper_routes")
data class RouteHelperRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val zipCode: String,
    val createdAtEpochMillis: Long
)

/** A candidate stop pulled from OpenStreetMap for a route's ZIP code - a real address until it's routed. */
@Entity(tableName = "candidate_addresses")
data class CandidateAddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val isRouted: Boolean = false
)

/** A stop actually added to the route, in sequence order, at the driver's live location when they tapped it. */
@Entity(tableName = "routed_stops")
data class RoutedStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val sequenceOrder: Int,
    val addressLabel: String,
    val note: String?,
    val latitude: Double,
    val longitude: Double,
    /** Null for a stop added without matching a fetched candidate (shouldn't normally happen, but keeps this optional rather than a hard FK). */
    val candidateAddressId: Long?,
    val createdAtEpochMillis: Long,
    /** Recipient's last name, when this stop was added by scanning a mail piece. Not otherwise used yet - reserved for a later feature. */
    val recipientLastName: String? = null
)

/** Cached OSRM road-following route for offline use. One row per route, storing the polyline as JSON. */
@Entity(tableName = "cached_road_routes")
data class CachedRoadRouteEntity(
    @PrimaryKey val routeId: Long,
    /** JSON array of lat/lng points: [[lat1,lng1],[lat2,lng2],...] */
    val polylineJson: String,
    val fetchedAtEpochMillis: Long
)

/** A scanned package with tracking number, scanned in the morning before route playback. */
@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    /** USPS tracking number extracted from package label */
    val trackingNumber: String,
    /** Address label extracted from package */
    val addressLabel: String,
    /** Matched routed stop ID, null if not yet matched */
    val routedStopId: Long? = null,
    /** Whether this package has been delivered (marked complete during route playback) */
    val isDelivered: Boolean = false,
    val scannedAtEpochMillis: Long
)

/** A named section of a route (e.g., "2B", "Around the lake"). Sections are defined by a range of stops. */
@Entity(tableName = "route_sections")
data class RouteSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    /** Display name for this section (e.g., "2B", "Around the lake") */
    val name: String,
    /** Stop ID where this section begins (inclusive) */
    val startStopId: Long,
    /** Stop ID where this section ends (inclusive) */
    val endStopId: Long,
    val createdAtEpochMillis: Long
)

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
    val createdAtEpochMillis: Long
)

package com.isaacshub.app.routehelper.data

import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.network.AddressFetchResult
import com.isaacshub.app.routehelper.network.OsmAddressFetcher
import kotlinx.coroutines.flow.Flow

sealed interface CreateRouteResult {
    data class Success(val routeId: Long, val addressCount: Int) : CreateRouteResult
    data class Failure(val reason: String) : CreateRouteResult
}

class RouteHelperRepository(
    private val dao: RouteHelperDao,
    private val addressFetcher: OsmAddressFetcher
) {
    fun observeRoutes(): Flow<List<RouteHelperRouteEntity>> = dao.observeRoutes()

    suspend fun getRoute(id: Long): RouteHelperRouteEntity? = dao.getRoute(id)

    suspend fun deleteRoute(route: RouteHelperRouteEntity) = dao.deleteRoute(route)

    /**
     * Creates the route and does the one-time OSM address fetch for its ZIP code. If the fetch
     * fails outright (network/server error, as opposed to a ZIP that genuinely has zero addressed
     * buildings mapped), the just-created route is rolled back so a failed attempt doesn't leave a
     * dead empty route behind - the caller can just retry.
     */
    suspend fun createRoute(name: String, zipCode: String): CreateRouteResult {
        val routeId = dao.insertRoute(
            RouteHelperRouteEntity(name = name, zipCode = zipCode, createdAtEpochMillis = System.currentTimeMillis())
        )
        return when (val fetched = addressFetcher.fetchAddressesForZip(zipCode)) {
            is AddressFetchResult.Success -> {
                if (fetched.addresses.isNotEmpty()) {
                    dao.insertCandidates(
                        fetched.addresses.map { address ->
                            CandidateAddressEntity(
                                routeId = routeId,
                                label = address.label,
                                latitude = address.location.latitude,
                                longitude = address.location.longitude
                            )
                        }
                    )
                }
                CreateRouteResult.Success(routeId, fetched.addresses.size)
            }
            is AddressFetchResult.Failure -> {
                dao.deleteRouteById(routeId)
                CreateRouteResult.Failure(fetched.reason)
            }
        }
    }

    fun observeUnroutedCandidates(routeId: Long): Flow<List<CandidateAddressEntity>> =
        dao.observeUnroutedCandidates(routeId)

    fun observeStops(routeId: Long): Flow<List<RoutedStopEntity>> = dao.observeStops(routeId)

    /** Adds a stop at the driver's current [location], marking its source candidate (if any) as routed. */
    suspend fun addStop(routeId: Long, candidateId: Long?, addressLabel: String, note: String?, location: GeoPoint) {
        val nextOrder = dao.maxSequenceOrder(routeId) + 1
        dao.insertStop(
            RoutedStopEntity(
                routeId = routeId,
                sequenceOrder = nextOrder,
                addressLabel = addressLabel,
                note = note,
                latitude = location.latitude,
                longitude = location.longitude,
                candidateAddressId = candidateId,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
        candidateId?.let { dao.setCandidateRouted(it, true) }
    }

    /** Removes the most recently routed stop and restores its candidate address to the unrouted list. */
    suspend fun undoLastStop(routeId: Long) {
        val last = dao.getLastStop(routeId) ?: return
        dao.deleteStop(last)
        last.candidateAddressId?.let { dao.setCandidateRouted(it, false) }
    }
}

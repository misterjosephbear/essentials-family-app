package com.isaacshub.app.routehelper.data

import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.network.AddressFetchResult
import com.isaacshub.app.routehelper.network.RouteHelperAddressFetcher
import kotlinx.coroutines.flow.Flow

sealed interface CreateRouteResult {
    data class Success(val routeId: Long, val addressCount: Int) : CreateRouteResult
    data class Failure(val reason: String) : CreateRouteResult
}

class RouteHelperRepository(
    private val dao: RouteHelperDao,
    private val addressFetcher: RouteHelperAddressFetcher
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

    suspend fun getStopsOnce(routeId: Long): List<RoutedStopEntity> = dao.getStopsOnce(routeId)

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

    /**
     * Inserts a new stop just before [beforeStopId] in sequence (null appends at the end), shifting
     * every stop from that point on back by one. Used to slot in a stop scanned off a mail piece
     * between the driver's last stop and whichever one they were heading to next.
     */
    suspend fun insertStopBefore(
        routeId: Long,
        beforeStopId: Long?,
        addressLabel: String,
        recipientLastName: String?,
        location: GeoPoint
    ) {
        val stops = dao.getStopsOnce(routeId)
        val insertIndex = beforeStopId?.let { id -> stops.indexOfFirst { it.id == id }.takeIf { it != -1 } } ?: stops.size
        val insertOrder = if (insertIndex < stops.size) {
            stops[insertIndex].sequenceOrder
        } else {
            dao.maxSequenceOrder(routeId) + 1
        }
        if (insertIndex < stops.size) {
            dao.updateStops(stops.drop(insertIndex).map { it.copy(sequenceOrder = it.sequenceOrder + 1) })
        }
        dao.insertStop(
            RoutedStopEntity(
                routeId = routeId,
                sequenceOrder = insertOrder,
                addressLabel = addressLabel,
                note = null,
                latitude = location.latitude,
                longitude = location.longitude,
                candidateAddressId = null,
                createdAtEpochMillis = System.currentTimeMillis(),
                recipientLastName = recipientLastName
            )
        )
    }

    /** Removes the most recently routed stop and restores its candidate address to the unrouted list. */
    suspend fun undoLastStop(routeId: Long) {
        val last = dao.getLastStop(routeId) ?: return
        dao.deleteStop(last)
        last.candidateAddressId?.let { dao.setCandidateRouted(it, false) }
    }

    /** Removes an arbitrary stop (not just the most recent) and restores its candidate address to the unrouted list. */
    suspend fun deleteStop(stop: RoutedStopEntity) {
        dao.deleteStop(stop)
        stop.candidateAddressId?.let { dao.setCandidateRouted(it, false) }
    }

    /** Swaps [stop] with the one immediately before it in sequence. No-op if it's already first. */
    suspend fun moveStopUp(routeId: Long, stop: RoutedStopEntity) {
        val stops = dao.getStopsOnce(routeId)
        val index = stops.indexOfFirst { it.id == stop.id }
        if (index <= 0) return
        swapOrder(stops[index], stops[index - 1])
    }

    /** Swaps [stop] with the one immediately after it in sequence. No-op if it's already last. */
    suspend fun moveStopDown(routeId: Long, stop: RoutedStopEntity) {
        val stops = dao.getStopsOnce(routeId)
        val index = stops.indexOfFirst { it.id == stop.id }
        if (index == -1 || index >= stops.size - 1) return
        swapOrder(stops[index], stops[index + 1])
    }

    private suspend fun swapOrder(a: RoutedStopEntity, b: RoutedStopEntity) {
        dao.updateStops(listOf(a.copy(sequenceOrder = b.sequenceOrder), b.copy(sequenceOrder = a.sequenceOrder)))
    }

    /** Gets cached road route polyline for offline use. */
    suspend fun getCachedRoadRoute(routeId: Long): CachedRoadRouteEntity? = dao.getCachedRoadRoute(routeId)

    /** Caches OSRM road route polyline for offline use. */
    suspend fun cacheRoadRoute(routeId: Long, points: List<GeoPoint>) {
        val json = points.joinToString(prefix = "[", postfix = "]") { "[${it.latitude},${it.longitude}]" }
        dao.insertCachedRoadRoute(
            CachedRoadRouteEntity(
                routeId = routeId,
                polylineJson = json,
                fetchedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    /** Deletes cached road route - used when stops change and route needs recalculation. */
    suspend fun deleteCachedRoadRoute(routeId: Long) = dao.deleteCachedRoadRoute(routeId)

    // Package management methods
    suspend fun addPackage(routeId: Long, trackingNumber: String, addressLabel: String): Long {
        return dao.insertPackage(
            PackageEntity(
                routeId = routeId,
                trackingNumber = trackingNumber,
                addressLabel = addressLabel,
                scannedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    /** Add a package that's already been matched to a stop during scanning */
    suspend fun addPackageWithStop(routeId: Long, trackingNumber: String, addressLabel: String, routedStopId: Long): Long {
        return dao.insertPackage(
            PackageEntity(
                routeId = routeId,
                trackingNumber = trackingNumber,
                addressLabel = addressLabel,
                routedStopId = routedStopId,
                scannedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    fun observePackages(routeId: Long): Flow<List<PackageEntity>> = dao.observePackages(routeId)

    fun observePackagesWithSequence(routeId: Long): Flow<List<PackageWithSequence>> = dao.observePackagesWithSequence(routeId)

    fun observeUndeliveredPackages(routeId: Long): Flow<List<PackageEntity>> = dao.observeUndeliveredPackages(routeId)

    suspend fun matchPackagesToStops(routeId: Long) {
        val packages = dao.observePackages(routeId)
        val stops = dao.getStopsOnce(routeId)

        android.util.Log.d("PackageMatching", "Attempting to match packages for route $routeId")
        android.util.Log.d("PackageMatching", "Found ${stops.size} stops in route")

        // Match packages to stops by comparing addresses
        packages.collect { pkgList ->
            android.util.Log.d("PackageMatching", "Matching ${pkgList.size} packages")

            for (pkg in pkgList) {
                if (pkg.routedStopId != null) {
                    android.util.Log.d("PackageMatching", "Package ${pkg.trackingNumber} already matched to stop ${pkg.routedStopId}")
                    continue  // Already matched
                }

                android.util.Log.d("PackageMatching", "Looking for match for package: ${pkg.addressLabel}")

                // Find best matching stop
                val matchedStop = stops.find { stop ->
                    val matches = addressesMatch(pkg.addressLabel, stop.addressLabel)
                    if (matches) {
                        android.util.Log.d("PackageMatching", "  ✓ Matched with stop: ${stop.addressLabel}")
                    }
                    matches
                }

                if (matchedStop != null) {
                    dao.setPackageStop(pkg.id, matchedStop.id)
                    android.util.Log.d("PackageMatching", "Package ${pkg.trackingNumber} matched to stop ${matchedStop.id}")
                } else {
                    android.util.Log.w("PackageMatching", "  ✗ No match found for package: ${pkg.addressLabel}")
                    android.util.Log.w("PackageMatching", "  Available stops:")
                    stops.forEach { stop ->
                        android.util.Log.w("PackageMatching", "    - ${stop.addressLabel}")
                    }
                }
            }
        }
    }

    suspend fun setPackageDelivered(packageId: Long, delivered: Boolean) {
        dao.setPackageDelivered(packageId, delivered)
    }

    suspend fun deletePackage(pkg: PackageEntity) = dao.deletePackage(pkg)

    fun observePackagesForStop(routeId: Long, stopId: Long): Flow<List<PackageEntity>> {
        return dao.observePackagesForStop(routeId, stopId)
    }

    suspend fun getUndeliveredPackageCountForStop(routeId: Long, stopId: Long): Int {
        return dao.getUndeliveredPackageCountForStop(routeId, stopId)
    }

    // Section management
    suspend fun addSection(routeId: Long, name: String, startStopId: Long, endStopId: Long): Long {
        return dao.insertSection(
            RouteSectionEntity(
                routeId = routeId,
                name = name,
                startStopId = startStopId,
                endStopId = endStopId,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    fun observeSections(routeId: Long): Flow<List<RouteSectionEntity>> = dao.observeSections(routeId)

    suspend fun getSectionsOnce(routeId: Long): List<RouteSectionEntity> = dao.getSectionsOnce(routeId)

    suspend fun deleteSection(section: RouteSectionEntity) = dao.deleteSection(section)

    suspend fun updateSection(section: RouteSectionEntity) = dao.updateSection(section)

    suspend fun getSectionsForStop(routeId: Long, stopId: Long): List<RouteSectionEntity> {
        return dao.getSectionsForStop(routeId, stopId)
    }

    /**
     * Check if two addresses are likely referring to the same location.
     * Handles different formats and minor variations.
     */
    private fun addressesMatch(addr1: String, addr2: String): Boolean {
        // Normalize addresses for comparison
        val normalized1 = addr1.lowercase()
            .replace(Regex("""[^\w\s]"""), "")  // Remove punctuation
            .replace(Regex("""\s+"""), " ")      // Normalize spaces
            .trim()

        val normalized2 = addr2.lowercase()
            .replace(Regex("""[^\w\s]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Extract street number and name
        val num1 = normalized1.split(" ").firstOrNull()
        val num2 = normalized2.split(" ").firstOrNull()

        // If numbers don't match, it's not the same address
        if (num1 != num2) return false

        // Check if street names are similar (simple contains check)
        // This handles cases like "123 Main St" vs "123 Main Street"
        return normalized1.contains(normalized2.substringAfter(" ").take(6)) ||
               normalized2.contains(normalized1.substringAfter(" ").take(6))
    }
}

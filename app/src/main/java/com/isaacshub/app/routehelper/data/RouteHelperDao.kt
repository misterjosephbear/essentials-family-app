package com.isaacshub.app.routehelper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteHelperDao {

    @Insert
    suspend fun insertRoute(route: RouteHelperRouteEntity): Long

    @Query("SELECT * FROM route_helper_routes ORDER BY createdAtEpochMillis DESC")
    fun observeRoutes(): Flow<List<RouteHelperRouteEntity>>

    @Query("SELECT * FROM route_helper_routes WHERE id = :id")
    suspend fun getRoute(id: Long): RouteHelperRouteEntity?

    @Delete
    suspend fun deleteRoute(route: RouteHelperRouteEntity)

    @Query("DELETE FROM route_helper_routes WHERE id = :id")
    suspend fun deleteRouteById(id: Long)

    @Insert
    suspend fun insertCandidates(candidates: List<CandidateAddressEntity>)

    @Query("SELECT * FROM candidate_addresses WHERE routeId = :routeId AND isRouted = 0")
    fun observeUnroutedCandidates(routeId: Long): Flow<List<CandidateAddressEntity>>

    @Query("SELECT * FROM candidate_addresses WHERE id = :id")
    suspend fun getCandidate(id: Long): CandidateAddressEntity?

    @Query("UPDATE candidate_addresses SET isRouted = :routed WHERE id = :id")
    suspend fun setCandidateRouted(id: Long, routed: Boolean)

    @Insert
    suspend fun insertStop(stop: RoutedStopEntity): Long

    @Query("SELECT * FROM routed_stops WHERE routeId = :routeId ORDER BY sequenceOrder ASC")
    fun observeStops(routeId: Long): Flow<List<RoutedStopEntity>>

    @Query("SELECT * FROM routed_stops WHERE routeId = :routeId ORDER BY sequenceOrder ASC")
    suspend fun getStopsOnce(routeId: Long): List<RoutedStopEntity>

    @Query("SELECT * FROM routed_stops WHERE routeId = :routeId ORDER BY sequenceOrder DESC LIMIT 1")
    suspend fun getLastStop(routeId: Long): RoutedStopEntity?

    @Delete
    suspend fun deleteStop(stop: RoutedStopEntity)

    /** Room wraps a list @Update in a single transaction, so a reordering swap can't land half-applied. */
    @Update
    suspend fun updateStops(stops: List<RoutedStopEntity>)

    @Query("UPDATE routed_stops SET expectedPackageCount = :quantity WHERE id = :stopId")
    suspend fun updateStopQuantity(stopId: Long, quantity: Int)

    @Query("SELECT COALESCE(MAX(sequenceOrder), -1) FROM routed_stops WHERE routeId = :routeId")
    suspend fun maxSequenceOrder(routeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRoadRoute(cachedRoute: CachedRoadRouteEntity)

    @Query("SELECT * FROM cached_road_routes WHERE routeId = :routeId")
    suspend fun getCachedRoadRoute(routeId: Long): CachedRoadRouteEntity?

    @Query("DELETE FROM cached_road_routes WHERE routeId = :routeId")
    suspend fun deleteCachedRoadRoute(routeId: Long)

    // Package queries
    @Insert
    suspend fun insertPackage(pkg: PackageEntity): Long

    @Query("SELECT * FROM packages WHERE routeId = :routeId ORDER BY scannedAtEpochMillis ASC")
    fun observePackages(routeId: Long): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE routeId = :routeId AND isDelivered = 0 ORDER BY scannedAtEpochMillis ASC")
    fun observeUndeliveredPackages(routeId: Long): Flow<List<PackageEntity>>

    @Query("""
        SELECT p.id, p.routeId, p.trackingNumber, p.addressLabel, p.routedStopId, p.isDelivered,
               p.scannedAtEpochMillis, s.sequenceOrder as sequenceNumber, p.isUnknownStreetMatch,
               p.streetName, p.plotAfterStopId, p.plotBeforeStopId, p.plottedLatitude, p.plottedLongitude
        FROM packages p
        LEFT JOIN routed_stops s ON p.routedStopId = s.id
        WHERE p.routeId = :routeId
        ORDER BY p.scannedAtEpochMillis ASC
    """)
    fun observePackagesWithSequence(routeId: Long): Flow<List<PackageWithSequence>>

    @Query("UPDATE packages SET routedStopId = :stopId WHERE id = :packageId")
    suspend fun setPackageStop(packageId: Long, stopId: Long?)

    @Query("UPDATE packages SET isDelivered = :delivered WHERE id = :packageId")
    suspend fun setPackageDelivered(packageId: Long, delivered: Boolean)

    @Query("SELECT * FROM packages WHERE routeId = :routeId AND routedStopId = :stopId AND isDelivered = 0")
    fun observePackagesForStop(routeId: Long, stopId: Long): Flow<List<PackageEntity>>

    @Query("SELECT COUNT(*) FROM packages WHERE routeId = :routeId AND routedStopId = :stopId AND isDelivered = 0")
    suspend fun getUndeliveredPackageCountForStop(routeId: Long, stopId: Long): Int

    @Delete
    suspend fun deletePackage(pkg: PackageEntity)

    @Query("DELETE FROM packages WHERE scannedAtEpochMillis < :beforeTimestampMillis")
    suspend fun deletePackagesScanBefore(beforeTimestampMillis: Long): Int

    @Query("UPDATE packages SET plotAfterStopId = :afterStopId, plotBeforeStopId = :beforeStopId WHERE routeId = :routeId AND trackingNumber = :trackingNumber")
    suspend fun setPackagePlotStops(routeId: Long, trackingNumber: String, afterStopId: Long, beforeStopId: Long)

    // Section queries
    @Insert
    suspend fun insertSection(section: RouteSectionEntity): Long

    @Query("SELECT * FROM route_sections WHERE routeId = :routeId ORDER BY name ASC")
    fun observeSections(routeId: Long): Flow<List<RouteSectionEntity>>

    @Query("SELECT * FROM route_sections WHERE routeId = :routeId ORDER BY name ASC")
    suspend fun getSectionsOnce(routeId: Long): List<RouteSectionEntity>

    @Delete
    suspend fun deleteSection(section: RouteSectionEntity)

    @Update
    suspend fun updateSection(section: RouteSectionEntity)

    /**
     * Get all sections that contain a specific stop.
     * A stop is in a section if it falls within the section's start and end stops by sequence order.
     */
    @Query("""
        SELECT s.* FROM route_sections s
        INNER JOIN routed_stops start_stop ON s.startStopId = start_stop.id
        INNER JOIN routed_stops end_stop ON s.endStopId = end_stop.id
        INNER JOIN routed_stops target_stop ON target_stop.id = :stopId
        WHERE s.routeId = :routeId
        AND target_stop.sequenceOrder >= start_stop.sequenceOrder
        AND target_stop.sequenceOrder <= end_stop.sequenceOrder
        ORDER BY s.name ASC
    """)
    suspend fun getSectionsForStop(routeId: Long, stopId: Long): List<RouteSectionEntity>
}

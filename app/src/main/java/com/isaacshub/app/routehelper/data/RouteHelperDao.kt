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

    @Query("SELECT COALESCE(MAX(sequenceOrder), -1) FROM routed_stops WHERE routeId = :routeId")
    suspend fun maxSequenceOrder(routeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRoadRoute(cachedRoute: CachedRoadRouteEntity)

    @Query("SELECT * FROM cached_road_routes WHERE routeId = :routeId")
    suspend fun getCachedRoadRoute(routeId: Long): CachedRoadRouteEntity?

    @Query("DELETE FROM cached_road_routes WHERE routeId = :routeId")
    suspend fun deleteCachedRoadRoute(routeId: Long)
}

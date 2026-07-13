package com.isaacshub.app.timetracking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteScheduleOverrideDao {

    @Insert
    suspend fun insert(override: RouteScheduleOverrideEntity): Long

    @Query("DELETE FROM route_schedule_overrides WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM route_schedule_overrides ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<RouteScheduleOverrideEntity>>
}

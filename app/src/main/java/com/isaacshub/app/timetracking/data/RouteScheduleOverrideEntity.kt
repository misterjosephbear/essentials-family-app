package com.isaacshub.app.timetracking.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A one-off day [routeId] is scheduled on, on top of (or independent from) its regular recurring schedule. */
@Entity(tableName = "route_schedule_overrides")
data class RouteScheduleOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    /** java.time.LocalDate.toEpochDay() of the scheduled date. */
    val epochDay: Long
)

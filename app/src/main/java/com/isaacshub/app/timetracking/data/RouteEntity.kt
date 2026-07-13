package com.isaacshub.app.timetracking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class ScheduleType { NONE, WEEKLY, BIWEEKLY, DAILY_SAT_FRI, DAILY_MON_FRI }

@Entity(tableName = "routes")
@TypeConverters(ScheduleTypeConverter::class)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeNumber: String,
    val cityName: String,
    val evaluatedHours: Double,
    val evaluatedMiles: Double,
    val scheduleType: ScheduleType = ScheduleType.NONE,
    /**
     * [java.time.LocalDate.toEpochDay] of one real occurrence of this route. Only used for WEEKLY/BIWEEKLY:
     * its day-of-week is the recurring day, and for BIWEEKLY its week parity anchors which of the two weeks
     * the route falls on.
     */
    val scheduleAnchorEpochDay: Long? = null,
    val notes: String? = null
)

class ScheduleTypeConverter {
    @TypeConverter
    fun fromScheduleType(type: ScheduleType): String = type.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)
}

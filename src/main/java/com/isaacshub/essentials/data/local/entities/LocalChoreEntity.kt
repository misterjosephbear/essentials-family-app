package com.isaacshub.essentials.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.DayOfWeek

@Entity(tableName = "local_chores")
data class LocalChoreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
    val photoRequirement: String?,
    val daysOfWeek: List<DayOfWeek>,
    val syncedAtEpochMillis: Long
)

class DayOfWeekListConverter {
    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekList(value: String): List<DayOfWeek> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { DayOfWeek.valueOf(it) }
    }
}

package com.isaacshub.app.essentials.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.DayOfWeek

@Entity(tableName = "chores")
@TypeConverters(DayOfWeekListConverter::class, LongListConverter::class)
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val photoRequirement: String?,
    val daysOfWeek: List<DayOfWeek>,
    val assignedChildIds: List<Long>,
    val createdAtEpochMillis: Long
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

class LongListConverter {
    @TypeConverter
    fun fromLongList(ids: List<Long>): String {
        return ids.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { it.toLong() }
    }
}

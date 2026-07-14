package com.isaacshub.app.sleep.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class SleepSource { AUTO_DETECTED, MANUAL, NAP }

@Entity(tableName = "sleep_sessions")
@TypeConverters(SleepSourceConverter::class)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val source: SleepSource,
    val confirmed: Boolean,
    val createdAtEpochMillis: Long
) {
    val durationMinutes: Long
        get() = (endEpochMillis - startEpochMillis) / 60_000L
}

class SleepSourceConverter {
    @TypeConverter
    fun fromSource(source: SleepSource): String = source.name

    @TypeConverter
    fun toSource(value: String): SleepSource = SleepSource.valueOf(value)
}

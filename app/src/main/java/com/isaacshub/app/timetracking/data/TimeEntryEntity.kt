package com.isaacshub.app.timetracking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class PayType { HOURLY, EVALUATION }

@Entity(tableName = "time_entries")
@TypeConverters(PayTypeConverter::class)
data class TimeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val payType: PayType,
    /** Route this entry was logged against, if any. Null for freeform hourly work (e.g. Amazon Sunday). */
    val routeId: Long?,
    /** Route number/city label, snapshotted at entry time so edits to the route library don't rewrite history. */
    val label: String,
    /**
     * Evaluated hours/miles snapshot from the route, only set for [PayType.EVALUATION]. This is what actually
     * gets paid for evaluated work - it does not move with actual clock time.
     */
    val evaluatedHours: Double?,
    val evaluatedMiles: Double?,
    val notes: String?,
    val createdAtEpochMillis: Long
) {
    /** Actual clocked hours from start to end. Always tracked, regardless of pay type - USPS overtime is based on actual hours worked, not evaluated time. */
    val actualHours: Double
        get() = (endEpochMillis - startEpochMillis) / 3_600_000.0
}

class PayTypeConverter {
    @TypeConverter
    fun fromPayType(payType: PayType): String = payType.name

    @TypeConverter
    fun toPayType(value: String): PayType = PayType.valueOf(value)
}

package com.isaacshub.app.timetracking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class DeductionType { FLAT, PERCENT }

@Entity(tableName = "deductions")
@TypeConverters(DeductionTypeConverter::class)
data class DeductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DeductionType,
    /** Dollar amount if [DeductionType.FLAT], percentage points (e.g. 6.2 for 6.2%) if [DeductionType.PERCENT]. */
    val amount: Double
)

class DeductionTypeConverter {
    @TypeConverter
    fun fromDeductionType(type: DeductionType): String = type.name

    @TypeConverter
    fun toDeductionType(value: String): DeductionType = DeductionType.valueOf(value)
}

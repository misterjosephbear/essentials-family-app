package com.isaacshub.app.essentials.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

enum class CompletionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "chore_completions")
@TypeConverters(CompletionStatusConverter::class, LocalDateConverter::class)
data class ChoreCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val childUserId: Long,
    val completionDate: LocalDate,
    val photoUri: String?,
    val aiVerificationResult: String?,
    val adminOverride: Boolean,
    val status: CompletionStatus,
    val completedAtEpochMillis: Long?
)

class CompletionStatusConverter {
    @TypeConverter
    fun fromStatus(status: CompletionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): CompletionStatus = CompletionStatus.valueOf(value)
}

class LocalDateConverter {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}

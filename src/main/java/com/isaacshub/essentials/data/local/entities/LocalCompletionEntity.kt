package com.isaacshub.essentials.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "local_completions")

data class LocalCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val completionDate: String, // LocalDate.toString()
    val photoUri: String?,
    val aiVerificationResult: String?,
    val status: CompletionStatus,
    val syncedToServer: Boolean = false,
    val completedAtEpochMillis: Long
)

enum class CompletionStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    COMPLETED
}

class CompletionStatusConverter {
    @TypeConverter
    fun fromStatus(status: CompletionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): CompletionStatus = CompletionStatus.valueOf(value)
}

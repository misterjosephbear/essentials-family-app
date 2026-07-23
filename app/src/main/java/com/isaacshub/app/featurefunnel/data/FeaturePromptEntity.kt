package com.isaacshub.app.featurefunnel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class PromptStatus {
    QUEUED,          // Waiting to be sent
    IN_PROGRESS,     // Sent to Claude, awaiting completion
    COMPLETED,       // Git commit detected
    FAILED,          // Failed after retries
    PAUSED           // Manually paused by user
}

class PromptStatusConverter {
    @TypeConverter
    fun fromStatus(status: PromptStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): PromptStatus = PromptStatus.valueOf(value)
}

@Entity(tableName = "feature_prompts")
@TypeConverters(PromptStatusConverter::class)
data class FeaturePromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,                           // Brief description for UI
    val promptText: String,                      // Full prompt to send to Claude
    val status: PromptStatus,
    val priority: Int = 0,                       // For ordering (higher = more important)
    val createdAtEpochMillis: Long,
    val sentAtEpochMillis: Long? = null,        // When prompt was sent to Discord
    val completedAtEpochMillis: Long? = null,    // When git commit detected
    val failureCount: Int = 0,                   // Number of failed attempts
    val lastFailureMessage: String? = null,      // Last error message
    val expectedCommitPattern: String? = null    // Optional regex for commit message
)

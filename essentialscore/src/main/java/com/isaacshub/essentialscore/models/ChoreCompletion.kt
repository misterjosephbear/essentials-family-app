package com.isaacshub.essentialscore.models

import kotlinx.serialization.Serializable

@Serializable
enum class CompletionStatus {
    NOT_STARTED,
    IN_PROGRESS, // Photo taken but not yet verified
    PENDING_VERIFICATION, // Uploaded, waiting for AI/admin review
    VERIFIED, // AI approved the photo
    REJECTED, // AI rejected the photo
    COMPLETED, // Admin override or AI verified
    FAILED // Could not complete (e.g., photo verification failed multiple times)
}

@Serializable
data class ChoreCompletion(
    val id: Long,
    val choreId: Long,
    val childUserId: Long,
    val completionDate: String, // ISO 8601 date string (YYYY-MM-DD)
    val photoUrl: String?,
    val aiVerificationResult: PhotoVerificationResult?,
    val adminOverride: Boolean,
    val status: CompletionStatus,
    val completedAt: Long? // Unix timestamp (null if not completed)
)

@Serializable
data class CompleteChoreRequest(
    val choreId: Long,
    val photoBase64: String? // Base64 encoded photo (null if photo not required)
)

@Serializable
data class AdminOverrideRequest(
    val completionId: Long,
    val approved: Boolean,
    val note: String?
)

@Serializable
data class TodayChoresResponse(
    val chores: List<ChoreWithCompletion>
)

@Serializable
data class ChoreWithCompletion(
    val chore: Chore,
    val completion: ChoreCompletion?
)

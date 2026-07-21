package com.isaacshub.essentials.data.repository

import android.util.Log
import com.isaacshub.essentials.data.local.dao.ChoreDao
import com.isaacshub.essentials.data.local.dao.CompletionDao
import com.isaacshub.essentials.data.local.entities.CompletionStatus
import com.isaacshub.essentials.data.local.entities.LocalChoreEntity
import com.isaacshub.essentials.data.local.entities.LocalCompletionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class EssentialsRepository(
    private val choreDao: ChoreDao,
    private val completionDao: CompletionDao,
    private val authRepository: AuthRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "EssentialsRepository"
    }

    // ============================================================================
    // Chore Operations
    // ============================================================================

    fun observeAllChores(): Flow<List<LocalChoreEntity>> = choreDao.observeAll()

    suspend fun getChoreById(id: Long): LocalChoreEntity? = choreDao.getById(id)

    /**
     * Sync chores from server to local database.
     * This is typically called after login or on pull-to-refresh.
     */
    suspend fun syncChoresFromServer(): Result<Unit> {
        val serverUrl = authRepository.getServerUrl() ?: return Result.failure(Exception("No server URL"))
        val authToken = authRepository.getAuthToken() ?: return Result.failure(Exception("Not logged in"))

        return runCatching {
            // TODO: Call server API to get chores assigned to this child
            // For now, return success - will implement when API client is ready
            Log.d(TAG, "Syncing chores from server for user ${authToken.userId}")
        }
    }

    // ============================================================================
    // Completion Operations
    // ============================================================================

    fun observeCompletionsByDate(date: LocalDate): Flow<List<LocalCompletionEntity>> =
        completionDao.observeByDate(date.toString())

    suspend fun getCompletionForChoreAndDate(choreId: Long, date: LocalDate): LocalCompletionEntity? =
        completionDao.getByChoreAndDate(choreId, date.toString())

    /**
     * Mark a chore as completed with optional photo.
     * The completion is saved locally and will be synced to server in background.
     */
    suspend fun markChoreCompleted(
        choreId: Long,
        date: LocalDate,
        photoUri: String?
    ): Long {
        val authToken = authRepository.getAuthToken() ?: throw Exception("Not logged in")

        val completion = LocalCompletionEntity(
            choreId = choreId,
            completionDate = date.toString(),
            photoUri = photoUri,
            aiVerificationResult = null,
            status = if (photoUri != null) CompletionStatus.PENDING_VERIFICATION else CompletionStatus.COMPLETED,
            syncedToServer = false,
            completedAtEpochMillis = Instant.now().toEpochMilli()
        )

        val id = completionDao.insert(completion)

        // Sync to server in background
        syncCompletionToServer(id)

        return id
    }

    /**
     * Submit a photo for a chore completion to the server for AI verification.
     */
    suspend fun submitPhotoForVerification(
        completionId: Long,
        photoBase64: String
    ): Result<String> {
        val serverUrl = authRepository.getServerUrl() ?: return Result.failure(Exception("No server URL"))
        val authToken = authRepository.getAuthToken() ?: return Result.failure(Exception("Not logged in"))

        return runCatching {
            // TODO: Call server API to submit photo and get AI verification
            // For now, return success - will implement when API integration is ready
            Log.d(TAG, "Submitting photo for completion $completionId")
            "Photo verification pending"
        }
    }

    private fun syncCompletionToServer(completionId: Long) {
        syncScope.launch {
            try {
                // TODO: Call server API to sync completion
                Log.d(TAG, "Syncing completion $completionId to server")

                // Mark as synced
                completionDao.markAsSynced(completionId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync completion to server", e)
            }
        }
    }

    /**
     * Sync any unsynced completions to the server.
     * This is called on app startup or when connection is restored.
     */
    suspend fun syncPendingCompletions(): Result<Unit> {
        return runCatching {
            val unsynced = completionDao.getUnsyncedCompletions()
            Log.d(TAG, "Syncing ${unsynced.size} pending completions")

            unsynced.forEach { completion ->
                syncCompletionToServer(completion.id)
            }
        }
    }

    /**
     * Get chores that are due today based on day of week.
     */
    suspend fun getChoresDueToday(): List<LocalChoreEntity> {
        val today = LocalDate.now().dayOfWeek
        val allChores = choreDao.observeAll()

        // This is a simplified version - in reality we'd query from Room
        // but since we can't filter by DayOfWeek directly, we'll do it in memory
        return emptyList() // TODO: Implement proper filtering
    }

    /**
     * Check if a specific chore is completed for a given date.
     */
    suspend fun isChoreCompleted(choreId: Long, date: LocalDate): Boolean {
        val completion = getCompletionForChoreAndDate(choreId, date)
        return completion != null && (
            completion.status == CompletionStatus.VERIFIED ||
            completion.status == CompletionStatus.COMPLETED
        )
    }
}

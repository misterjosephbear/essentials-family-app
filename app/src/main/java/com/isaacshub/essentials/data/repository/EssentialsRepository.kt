package com.isaacshub.essentials.data.repository

import android.net.Uri
import android.util.Log
import com.isaacshub.essentials.data.api.EssentialsApiClient
import com.isaacshub.essentials.data.local.dao.ChoreDao
import com.isaacshub.essentials.data.local.dao.CompletionDao
import com.isaacshub.essentials.data.local.entities.CompletionStatus
import com.isaacshub.essentials.data.local.entities.LocalChoreEntity
import com.isaacshub.essentials.data.local.entities.LocalCompletionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class EssentialsRepository(
    private val choreDao: ChoreDao,
    private val completionDao: CompletionDao,
    private val authRepository: AuthRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getApiClient(): EssentialsApiClient? {
        val serverUrl = authRepository.getServerUrl() ?: return null
        val authToken = authRepository.getAuthToken() ?: return null
        return EssentialsApiClient(serverUrl, authToken.token)
    }

    companion object {
        private const val TAG = "EssentialsRepository"
    }

    // ============================================================================
    // Chore Operations
    // ============================================================================

    fun observeAllChores(): Flow<List<LocalChoreEntity>> = choreDao.observeAll()

    fun observeChoreById(id: Long): Flow<LocalChoreEntity?> = choreDao.observeById(id)

    suspend fun getChoreById(id: Long): LocalChoreEntity? = choreDao.getById(id)

    /**
     * Sync chores from server to local database.
     * This is typically called after login or on pull-to-refresh.
     */
    suspend fun syncChoresFromServer(): Result<Unit> {
        val apiClient = getApiClient() ?: return Result.failure(Exception("Not logged in"))

        return apiClient.fetchChores()
            .mapCatching { choreDtos ->
                val chores = choreDtos.map { dto ->
                    LocalChoreEntity(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        photoRequirement = dto.photoRequirement,
                        daysOfWeek = dto.daysOfWeek.map { DayOfWeek.valueOf(it) },
                        syncedAtEpochMillis = Instant.now().toEpochMilli()
                    )
                }

                choreDao.deleteAll()
                choreDao.insertAll(chores)

                Log.d(TAG, "Synced ${chores.size} chores from server")
            }
    }

    // ============================================================================
    // Completion Operations
    // ============================================================================

    fun observeCompletionsByDate(date: LocalDate): Flow<List<LocalCompletionEntity>> =
        completionDao.observeByDate(date.toString())

    fun observeCompletionByChoreAndDate(choreId: Long, date: LocalDate): Flow<LocalCompletionEntity?> =
        completionDao.observeByChoreAndDate(choreId, date.toString())

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

        // If there's a photo, trigger AI verification in background
        if (photoUri != null) {
            triggerPhotoVerification(id, choreId, photoUri)
        } else {
            // No photo, just sync to server
            syncCompletionToServer(id)
        }

        return id
    }

    /**
     * Trigger photo verification in the background.
     * This will submit the photo to the server for AI analysis.
     */
    private fun triggerPhotoVerification(
        completionId: Long,
        choreId: Long,
        photoUriString: String
    ) {
        syncScope.launch {
            try {
                val apiClient = getApiClient()
                if (apiClient == null) {
                    Log.w(TAG, "Cannot verify photo: not logged in")
                    return@launch
                }

                // Get chore to know photo requirement
                val chore = choreDao.getById(choreId)
                if (chore == null) {
                    Log.w(TAG, "Cannot verify photo: chore not found")
                    return@launch
                }

                val photoRequirement = chore.photoRequirement
                if (photoRequirement == null) {
                    Log.w(TAG, "Cannot verify photo: no photo requirement")
                    return@launch
                }

                // Convert URI to File
                val photoFile = File(Uri.parse(photoUriString).path ?: return@launch)
                if (!photoFile.exists()) {
                    Log.w(TAG, "Cannot verify photo: file not found at $photoUriString")
                    return@launch
                }

                Log.d(TAG, "Submitting photo for AI verification: completion=$completionId")

                // Call API to verify photo
                apiClient.verifyPhoto(completionId, choreId, photoFile, photoRequirement)
                    .onSuccess { result ->
                        // Update completion with verification result
                        val completion = completionDao.getByChoreAndDate(
                            choreId,
                            LocalDate.now().toString()
                        )

                        if (completion != null) {
                            val updatedCompletion = completion.copy(
                                status = if (result.verified) CompletionStatus.VERIFIED
                                         else CompletionStatus.REJECTED,
                                aiVerificationResult = result.feedback
                            )
                            completionDao.update(updatedCompletion)

                            Log.d(TAG, "Photo verification complete: verified=${result.verified}")
                        }

                        // Mark as synced
                        completionDao.markAsSynced(completionId)
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Photo verification failed", error)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error during photo verification", e)
            }
        }
    }

    private fun syncCompletionToServer(completionId: Long) {
        syncScope.launch {
            try {
                val apiClient = getApiClient()
                if (apiClient == null) {
                    Log.w(TAG, "Cannot sync completion: not logged in")
                    return@launch
                }

                // Get completion data
                val completion = completionDao.getUnsyncedCompletions().find { it.id == completionId }
                if (completion == null) {
                    Log.w(TAG, "Completion not found or already synced: $completionId")
                    return@launch
                }

                // Sync to server
                apiClient.syncCompletion(
                    choreId = completion.choreId,
                    completionDate = completion.completionDate,
                    photoUri = completion.photoUri,
                    completedAtEpochMillis = completion.completedAtEpochMillis
                ).onSuccess {
                    completionDao.markAsSynced(completionId)
                    Log.d(TAG, "Completion synced successfully: $completionId")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to sync completion to server", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing completion", e)
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

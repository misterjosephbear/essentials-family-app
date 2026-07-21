package com.isaacshub.app.essentials.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class EssentialsRepository(
    private val choreDao: ChoreDao,
    private val childAccountDao: ChildAccountDao,
    private val choreCompletionDao: ChoreCompletionDao,
    private val apiClient: EssentialsApiClient? = null
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "EssentialsRepository"
    }

    // ============================================================================
    // Sync Operations
    // ============================================================================

    /** Syncs chores from server to local database */
    suspend fun syncChoresFromServer() {
        apiClient?.getChores()?.onSuccess { choreDtos ->
            Log.d(TAG, "Syncing ${choreDtos.size} chores from server")
            choreDtos.forEach { dto ->
                // Simple strategy: insert/update based on ID
                val entity = ChoreEntity(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    photoRequirement = dto.photoRequirement,
                    daysOfWeek = dto.daysOfWeek.map { DayOfWeek.valueOf(it) },
                    assignedChildIds = dto.assignedChildIds,
                    createdAtEpochMillis = dto.createdAt
                )
                choreDao.insert(entity)
            }
        }?.onFailure { error ->
            Log.e(TAG, "Failed to sync chores from server", error)
        }
    }

    /** Syncs children from server to local database */
    suspend fun syncChildrenFromServer() {
        apiClient?.getChildren()?.onSuccess { childDtos ->
            Log.d(TAG, "Syncing ${childDtos.size} children from server")
            childDtos.forEach { dto ->
                val entity = ChildAccountEntity(
                    id = dto.id,
                    username = dto.username,
                    displayName = dto.displayName,
                    createdAtEpochMillis = dto.createdAt
                )
                childAccountDao.insert(entity)
            }
        }?.onFailure { error ->
            Log.e(TAG, "Failed to sync children from server", error)
        }
    }

    /** Background sync - does not block UI */
    private fun syncInBackground(operation: suspend () -> Unit) {
        syncScope.launch {
            try {
                operation()
            } catch (e: Exception) {
                Log.e(TAG, "Background sync failed", e)
            }
        }
    }

    // ============================================================================
    // Chore Operations
    // ============================================================================

    fun observeAllChores(): Flow<List<ChoreEntity>> = choreDao.observeAll()

    suspend fun getChoreById(id: Long): ChoreEntity? = choreDao.getById(id)

    suspend fun createChore(
        name: String,
        description: String,
        photoRequirement: String?,
        daysOfWeek: List<DayOfWeek>,
        assignedChildIds: List<Long>
    ): Long {
        require(name.isNotBlank()) { "Chore name cannot be blank" }
        require(daysOfWeek.isNotEmpty()) { "Must select at least one day of the week" }

        val id = choreDao.insert(
            ChoreEntity(
                name = name,
                description = description,
                photoRequirement = photoRequirement,
                daysOfWeek = daysOfWeek,
                assignedChildIds = assignedChildIds,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )

        // Sync to server in background
        syncInBackground {
            apiClient?.createChore(
                name = name,
                description = description,
                photoRequirement = photoRequirement,
                daysOfWeek = daysOfWeek.map { it.name },
                assignedChildIds = assignedChildIds
            )?.onFailure { error ->
                Log.e(TAG, "Failed to sync created chore to server", error)
            }
        }

        return id
    }

    suspend fun updateChore(
        id: Long,
        name: String,
        description: String,
        photoRequirement: String?,
        daysOfWeek: List<DayOfWeek>,
        assignedChildIds: List<Long>
    ) {
        require(name.isNotBlank()) { "Chore name cannot be blank" }
        require(daysOfWeek.isNotEmpty()) { "Must select at least one day of the week" }

        val existing = choreDao.getById(id) ?: return
        choreDao.update(
            existing.copy(
                name = name,
                description = description,
                photoRequirement = photoRequirement,
                daysOfWeek = daysOfWeek,
                assignedChildIds = assignedChildIds
            )
        )

        // Sync to server in background
        syncInBackground {
            apiClient?.updateChore(
                id = id,
                name = name,
                description = description,
                photoRequirement = photoRequirement,
                daysOfWeek = daysOfWeek.map { it.name },
                assignedChildIds = assignedChildIds
            )?.onFailure { error ->
                Log.e(TAG, "Failed to sync updated chore to server", error)
            }
        }
    }

    suspend fun deleteChore(id: Long) {
        choreDao.deleteById(id)

        // Sync to server in background
        syncInBackground {
            apiClient?.deleteChore(id)?.onFailure { error ->
                Log.e(TAG, "Failed to sync chore deletion to server", error)
            }
        }
    }

    // Child account operations
    fun observeAllChildren(): Flow<List<ChildAccountEntity>> = childAccountDao.observeAll()

    suspend fun getChildById(id: Long): ChildAccountEntity? = childAccountDao.getById(id)

    suspend fun createChildAccount(
        username: String,
        displayName: String,
        password: String = "changeme"
    ): Long {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(displayName.isNotBlank()) { "Display name cannot be blank" }

        // Check if username already exists
        val existing = childAccountDao.getByUsername(username)
        require(existing == null) { "Username already exists" }

        val id = childAccountDao.insert(
            ChildAccountEntity(
                username = username,
                displayName = displayName,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )

        // Sync to server in background
        syncInBackground {
            apiClient?.createChild(
                username = username,
                displayName = displayName,
                password = password
            )?.onFailure { error ->
                Log.e(TAG, "Failed to sync created child account to server", error)
            }
        }

        return id
    }

    suspend fun updateChildAccount(
        id: Long,
        username: String,
        displayName: String,
        password: String? = null
    ) {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(displayName.isNotBlank()) { "Display name cannot be blank" }

        val existing = childAccountDao.getById(id) ?: return

        // Check if username already taken by another account
        val usernameCheck = childAccountDao.getByUsername(username)
        require(usernameCheck == null || usernameCheck.id == id) { "Username already exists" }

        childAccountDao.update(
            existing.copy(
                username = username,
                displayName = displayName
            )
        )

        // Sync to server in background
        syncInBackground {
            apiClient?.updateChild(
                id = id,
                username = username,
                displayName = displayName,
                password = password
            )?.onFailure { error ->
                Log.e(TAG, "Failed to sync updated child account to server", error)
            }
        }
    }

    suspend fun deleteChildAccount(id: Long) {
        val account = childAccountDao.getById(id) ?: return
        childAccountDao.delete(account)

        // Sync to server in background
        syncInBackground {
            apiClient?.deleteChild(id)?.onFailure { error ->
                Log.e(TAG, "Failed to sync child account deletion to server", error)
            }
        }
    }

    // Chore completion operations
    suspend fun markChoreCompleted(
        choreId: Long,
        childUserId: Long,
        date: LocalDate,
        photoUri: String?,
        aiVerificationResult: String?
    ): Long {
        val status = when {
            photoUri != null && aiVerificationResult != null -> CompletionStatus.VERIFIED
            photoUri != null -> CompletionStatus.PENDING_VERIFICATION
            else -> CompletionStatus.COMPLETED
        }

        return choreCompletionDao.insert(
            ChoreCompletionEntity(
                choreId = choreId,
                childUserId = childUserId,
                completionDate = date,
                photoUri = photoUri,
                aiVerificationResult = aiVerificationResult,
                adminOverride = false,
                status = status,
                completedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun getCompletionForChoreAndDate(choreId: Long, date: LocalDate): ChoreCompletionEntity? =
        choreCompletionDao.getByChoreAndDate(choreId, date.toString())

    fun observeCompletionsByDate(date: LocalDate): Flow<List<ChoreCompletionEntity>> =
        choreCompletionDao.observeByDate(date.toString())

    fun observePendingVerifications(): Flow<List<ChoreCompletionEntity>> =
        choreCompletionDao.observeByStatus(CompletionStatus.PENDING_VERIFICATION.name)

    suspend fun adminOverrideCompletion(choreId: Long, date: LocalDate, approved: Boolean) {
        val completion = choreCompletionDao.getByChoreAndDate(choreId, date.toString()) ?: return
        choreCompletionDao.update(
            completion.copy(
                adminOverride = true,
                status = if (approved) CompletionStatus.COMPLETED else CompletionStatus.REJECTED
            )
        )
    }
}

package com.isaacshub.app.essentials.data

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class EssentialsRepository(
    private val choreDao: ChoreDao,
    private val childAccountDao: ChildAccountDao,
    private val choreCompletionDao: ChoreCompletionDao
) {

    // Chore operations
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

        return choreDao.insert(
            ChoreEntity(
                name = name,
                description = description,
                photoRequirement = photoRequirement,
                daysOfWeek = daysOfWeek,
                assignedChildIds = assignedChildIds,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
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
    }

    suspend fun deleteChore(id: Long) = choreDao.deleteById(id)

    // Child account operations
    fun observeAllChildren(): Flow<List<ChildAccountEntity>> = childAccountDao.observeAll()

    suspend fun getChildById(id: Long): ChildAccountEntity? = childAccountDao.getById(id)

    suspend fun createChildAccount(
        username: String,
        displayName: String
    ): Long {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(displayName.isNotBlank()) { "Display name cannot be blank" }

        // Check if username already exists
        val existing = childAccountDao.getByUsername(username)
        require(existing == null) { "Username already exists" }

        return childAccountDao.insert(
            ChildAccountEntity(
                username = username,
                displayName = displayName,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun updateChildAccount(
        id: Long,
        username: String,
        displayName: String
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
    }

    suspend fun deleteChildAccount(id: Long) {
        val account = childAccountDao.getById(id) ?: return
        childAccountDao.delete(account)
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

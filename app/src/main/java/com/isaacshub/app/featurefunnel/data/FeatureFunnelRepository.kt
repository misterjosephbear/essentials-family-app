package com.isaacshub.app.featurefunnel.data

import kotlinx.coroutines.flow.Flow

class FeatureFunnelRepository(
    private val dao: FeaturePromptDao,
    private val prefsRepository: FeatureFunnelPreferencesRepository
) {

    fun observeAll(): Flow<List<FeaturePromptEntity>> = dao.observeAll()

    fun observeQueued(): Flow<List<FeaturePromptEntity>> =
        dao.observeByStatus(PromptStatus.QUEUED)

    fun observeInProgress(): Flow<List<FeaturePromptEntity>> =
        dao.observeByStatus(PromptStatus.IN_PROGRESS)

    fun observeCompleted(): Flow<List<FeaturePromptEntity>> =
        dao.observeByStatus(PromptStatus.COMPLETED)

    suspend fun getNextPromptToSend(): FeaturePromptEntity? = dao.getNextQueued()

    suspend fun getCurrentInProgress(): FeaturePromptEntity? = dao.getInProgress()

    suspend fun getById(id: Long): FeaturePromptEntity? = dao.getById(id)

    suspend fun createPrompt(title: String, promptText: String, priority: Int = 0): Long {
        return dao.insert(
            FeaturePromptEntity(
                title = title,
                promptText = promptText,
                status = PromptStatus.QUEUED,
                priority = priority,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePrompt(prompt: FeaturePromptEntity) = dao.update(prompt)

    suspend fun deletePrompt(prompt: FeaturePromptEntity) = dao.delete(prompt)

    suspend fun markPromptSent(id: Long) {
        val prompt = dao.getById(id) ?: return
        dao.update(
            prompt.copy(
                status = PromptStatus.IN_PROGRESS,
                sentAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun markPromptCompleted(id: Long) {
        val prompt = dao.getById(id) ?: return
        dao.update(
            prompt.copy(
                status = PromptStatus.COMPLETED,
                completedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun markPromptFailed(id: Long, errorMessage: String) {
        val prompt = dao.getById(id) ?: return
        dao.update(
            prompt.copy(
                status = if (prompt.failureCount >= 2) PromptStatus.FAILED else PromptStatus.QUEUED,
                failureCount = prompt.failureCount + 1,
                lastFailureMessage = errorMessage,
                sentAtEpochMillis = null  // Clear sent time so it can be retried
            )
        )
    }

    suspend fun retryPrompt(id: Long) {
        val prompt = dao.getById(id) ?: return
        dao.update(
            prompt.copy(
                status = PromptStatus.QUEUED,
                failureCount = 0,
                lastFailureMessage = null,
                sentAtEpochMillis = null
            )
        )
    }

    suspend fun pausePrompt(id: Long) {
        dao.updateStatus(id, PromptStatus.PAUSED)
    }

    suspend fun resumePrompt(id: Long) {
        dao.updateStatus(id, PromptStatus.QUEUED)
    }
}

package com.isaacshub.app.featurefunnel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeaturePromptDao {

    @Query("SELECT * FROM feature_prompts ORDER BY priority DESC, createdAtEpochMillis ASC")
    fun observeAll(): Flow<List<FeaturePromptEntity>>

    @Query("SELECT * FROM feature_prompts WHERE status = :status ORDER BY priority DESC, createdAtEpochMillis ASC")
    fun observeByStatus(status: PromptStatus): Flow<List<FeaturePromptEntity>>

    @Query("SELECT * FROM feature_prompts WHERE status = 'QUEUED' ORDER BY priority DESC, createdAtEpochMillis ASC LIMIT 1")
    suspend fun getNextQueued(): FeaturePromptEntity?

    @Query("SELECT * FROM feature_prompts WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getInProgress(): FeaturePromptEntity?

    @Query("SELECT * FROM feature_prompts WHERE id = :id")
    suspend fun getById(id: Long): FeaturePromptEntity?

    @Insert
    suspend fun insert(prompt: FeaturePromptEntity): Long

    @Update
    suspend fun update(prompt: FeaturePromptEntity)

    @Delete
    suspend fun delete(prompt: FeaturePromptEntity)

    @Query("UPDATE feature_prompts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PromptStatus)
}

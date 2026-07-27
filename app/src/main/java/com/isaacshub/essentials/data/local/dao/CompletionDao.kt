package com.isaacshub.essentials.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.isaacshub.essentials.data.local.entities.CompletionStatus
import com.isaacshub.essentials.data.local.entities.LocalCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Query("SELECT * FROM local_completions WHERE completionDate = :dateString")
    fun observeByDate(dateString: String): Flow<List<LocalCompletionEntity>>

    @Query("SELECT * FROM local_completions WHERE choreId = :choreId AND completionDate = :dateString")
    fun observeByChoreAndDate(choreId: Long, dateString: String): Flow<LocalCompletionEntity?>

    @Query("SELECT * FROM local_completions WHERE choreId = :choreId AND completionDate = :dateString")
    suspend fun getByChoreAndDate(choreId: Long, dateString: String): LocalCompletionEntity?

    @Query("SELECT * FROM local_completions WHERE syncedToServer = 0")
    suspend fun getUnsyncedCompletions(): List<LocalCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: LocalCompletionEntity): Long

    @Update
    suspend fun update(completion: LocalCompletionEntity)

    @Query("UPDATE local_completions SET syncedToServer = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}

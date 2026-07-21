package com.isaacshub.app.essentials.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreCompletionDao {

    @Insert
    suspend fun insert(completion: ChoreCompletionEntity): Long

    @Update
    suspend fun update(completion: ChoreCompletionEntity)

    @Delete
    suspend fun delete(completion: ChoreCompletionEntity)

    @Query("SELECT * FROM chore_completions WHERE choreId = :choreId AND completionDate = :dateString")
    suspend fun getByChoreAndDate(choreId: Long, dateString: String): ChoreCompletionEntity?

    @Query("SELECT * FROM chore_completions WHERE childUserId = :childId AND completionDate = :dateString")
    fun observeByChildAndDate(childId: Long, dateString: String): Flow<List<ChoreCompletionEntity>>

    @Query("SELECT * FROM chore_completions WHERE completionDate = :dateString")
    fun observeByDate(dateString: String): Flow<List<ChoreCompletionEntity>>

    @Query("SELECT * FROM chore_completions WHERE status = :statusName")
    fun observeByStatus(statusName: String): Flow<List<ChoreCompletionEntity>>
}

package com.isaacshub.app.essentials.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {

    @Insert
    suspend fun insert(chore: ChoreEntity): Long

    @Update
    suspend fun update(chore: ChoreEntity)

    @Delete
    suspend fun delete(chore: ChoreEntity)

    @Query("SELECT * FROM chores ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<ChoreEntity>>

    @Query("SELECT * FROM chores WHERE id = :id")
    suspend fun getById(id: Long): ChoreEntity?

    @Query("DELETE FROM chores WHERE id = :id")
    suspend fun deleteById(id: Long)
}

package com.isaacshub.essentials.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.isaacshub.essentials.data.local.entities.LocalChoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {

    @Query("SELECT * FROM local_chores")
    fun observeAll(): Flow<List<LocalChoreEntity>>

    @Query("SELECT * FROM local_chores WHERE id = :id")
    fun observeById(id: Long): Flow<LocalChoreEntity?>

    @Query("SELECT * FROM local_chores WHERE id = :id")
    suspend fun getById(id: Long): LocalChoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chore: LocalChoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chores: List<LocalChoreEntity>)

    @Query("DELETE FROM local_chores")
    suspend fun deleteAll()
}

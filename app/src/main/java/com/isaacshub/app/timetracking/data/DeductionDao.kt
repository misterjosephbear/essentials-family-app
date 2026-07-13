package com.isaacshub.app.timetracking.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeductionDao {

    @Insert
    suspend fun insert(deduction: DeductionEntity): Long

    @Delete
    suspend fun delete(deduction: DeductionEntity)

    @Query("SELECT * FROM deductions ORDER BY id ASC")
    fun observeAll(): Flow<List<DeductionEntity>>
}

package com.isaacshub.app.essentials.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildAccountDao {

    @Insert
    suspend fun insert(account: ChildAccountEntity): Long

    @Update
    suspend fun update(account: ChildAccountEntity)

    @Delete
    suspend fun delete(account: ChildAccountEntity)

    @Query("SELECT * FROM child_accounts ORDER BY displayName ASC")
    fun observeAll(): Flow<List<ChildAccountEntity>>

    @Query("SELECT * FROM child_accounts WHERE id = :id")
    suspend fun getById(id: Long): ChildAccountEntity?

    @Query("SELECT * FROM child_accounts WHERE username = :username")
    suspend fun getByUsername(username: String): ChildAccountEntity?
}

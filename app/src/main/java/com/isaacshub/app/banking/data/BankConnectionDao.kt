package com.isaacshub.app.banking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: BankConnectionEntity)

    @Query("SELECT * FROM bank_connections")
    fun getAllConnections(): Flow<List<BankConnectionEntity>>

    @Query("SELECT * FROM bank_connections WHERE id = :id")
    suspend fun getConnection(id: String): BankConnectionEntity?

    @Query("DELETE FROM bank_connections WHERE id = :id")
    suspend fun deleteConnection(id: String)

    @Query("UPDATE bank_connections SET lastSynced = :timestamp WHERE id = :id")
    suspend fun updateLastSynced(id: String, timestamp: Long)
}

package com.isaacshub.app.banking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BankingDao {
    // Connections
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

    // Accounts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<BankAccountEntity>)

    @Query("SELECT * FROM bank_accounts ORDER BY institutionName, accountName")
    fun getAllAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE connectionId = :connectionId")
    fun getAccountsByConnection(connectionId: String): Flow<List<BankAccountEntity>>

    @Query("DELETE FROM bank_accounts WHERE connectionId = :connectionId")
    suspend fun deleteAccountsByConnection(connectionId: String)

    @Transaction
    suspend fun deleteConnectionAndAccounts(connectionId: String) {
        deleteAccountsByConnection(connectionId)
        deleteConnection(connectionId)
    }
}

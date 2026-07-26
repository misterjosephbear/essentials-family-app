package com.isaacshub.app.banking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
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
}

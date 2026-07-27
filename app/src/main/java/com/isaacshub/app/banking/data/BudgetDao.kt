package com.isaacshub.app.banking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    // Categories
    @Query("SELECT * FROM budget_categories ORDER BY `order` ASC")
    fun getAllCategories(): Flow<List<BudgetCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: BudgetCategoryEntity)

    @Update
    suspend fun updateCategory(category: BudgetCategoryEntity)

    @Query("DELETE FROM budget_categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    // Account Selections
    @Query("SELECT * FROM budget_account_selections WHERE isIncluded = 1")
    fun getSelectedAccountIds(): Flow<List<BudgetAccountSelectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountSelection(selection: BudgetAccountSelectionEntity)

    @Query("UPDATE budget_account_selections SET isIncluded = :isIncluded WHERE accountId = :accountId")
    suspend fun updateAccountSelection(accountId: String, isIncluded: Boolean)

    @Query("SELECT * FROM budget_account_selections WHERE accountId = :accountId")
    suspend fun getAccountSelection(accountId: String): BudgetAccountSelectionEntity?

    // Combined query for budget calculation
    @Transaction
    @Query("""
        SELECT ba.* FROM bank_accounts ba
        INNER JOIN budget_account_selections bas ON ba.id = bas.accountId
        WHERE bas.isIncluded = 1
    """)
    fun getSelectedAccounts(): Flow<List<BankAccountEntity>>
}

package com.isaacshub.app.banking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BankConnectionEntity::class,
        BankAccountEntity::class,
        BudgetCategoryEntity::class,
        BudgetAccountSelectionEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class BankingDatabase : RoomDatabase() {

    abstract fun bankingDao(): BankingDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var instance: BankingDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create budget_categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        threshold REAL NOT NULL,
                        `order` INTEGER NOT NULL,
                        colorHex TEXT NOT NULL,
                        icon TEXT NOT NULL
                    )
                """)

                // Create budget_account_selections table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_account_selections (
                        accountId TEXT PRIMARY KEY NOT NULL,
                        isIncluded INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(accountId) REFERENCES bank_accounts(id) ON DELETE CASCADE
                    )
                """)

                // Insert default categories
                database.execSQL("""
                    INSERT INTO budget_categories VALUES
                    ('crucial', 'Crucial Budget', 2000.0, 0, '#FF6200EE', '🏠'),
                    ('utility', 'Utility Budget', 1500.0, 1, '#FF03DAC5', '🔧'),
                    ('convenience', 'Convenience Budget', 1000.0, 2, '#FF018786', '🎁'),
                    ('frivolous', 'Frivolous Budget', 500.0, 3, '#FFB00020', '💎')
                """)
            }
        }

        fun getInstance(context: Context): BankingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BankingDatabase::class.java,
                    "banking.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
            }
    }
}

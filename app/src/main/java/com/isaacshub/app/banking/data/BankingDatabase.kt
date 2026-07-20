package com.isaacshub.app.banking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BankConnectionEntity::class, BankAccountEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BankingDatabase : RoomDatabase() {

    abstract fun bankingDao(): BankingDao

    companion object {
        @Volatile private var instance: BankingDatabase? = null

        fun getInstance(context: Context): BankingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BankingDatabase::class.java,
                    "banking.db"
                ).build().also { instance = it }
            }
    }
}

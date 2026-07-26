package com.isaacshub.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.isaacshub.app.banking.data.BankAccountDao
import com.isaacshub.app.banking.data.BankAccountEntity
import com.isaacshub.app.banking.data.BankConnectionDao
import com.isaacshub.app.banking.data.BankConnectionEntity
import com.isaacshub.app.featurefunnel.data.FeaturePromptDao
import com.isaacshub.app.featurefunnel.data.FeaturePromptEntity
import com.isaacshub.app.sleep.data.SleepSessionDao
import com.isaacshub.app.sleep.data.SleepSessionEntity

/**
 * Consolidated database for general app features.
 * Combines Sleep, Banking, and FeatureFunnel databases.
 */
@Database(
    entities = [
        SleepSessionEntity::class,
        BankConnectionEntity::class,
        BankAccountEntity::class,
        FeaturePromptEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun bankConnectionDao(): BankConnectionDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun featurePromptDao(): FeaturePromptDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                ).build().also { instance = it }
            }
    }
}

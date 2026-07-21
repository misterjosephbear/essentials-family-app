package com.isaacshub.app.essentials.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChoreEntity::class,
        ChildAccountEntity::class,
        ChoreCompletionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EssentialsDatabase : RoomDatabase() {

    abstract fun choreDao(): ChoreDao
    abstract fun childAccountDao(): ChildAccountDao
    abstract fun choreCompletionDao(): ChoreCompletionDao

    companion object {
        @Volatile private var instance: EssentialsDatabase? = null

        fun getInstance(context: Context): EssentialsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EssentialsDatabase::class.java,
                    "essentials.db"
                ).build().also { instance = it }
            }
    }
}

package com.isaacshub.essentials.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.isaacshub.essentials.data.local.dao.ChoreDao
import com.isaacshub.essentials.data.local.dao.CompletionDao
import com.isaacshub.essentials.data.local.entities.CompletionStatusConverter
import com.isaacshub.essentials.data.local.entities.DayOfWeekListConverter
import com.isaacshub.essentials.data.local.entities.LocalChoreEntity
import com.isaacshub.essentials.data.local.entities.LocalCompletionEntity

@Database(
    entities = [
        LocalChoreEntity::class,
        LocalCompletionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DayOfWeekListConverter::class, CompletionStatusConverter::class)
abstract class EssentialsDatabase : RoomDatabase() {
    abstract fun choreDao(): ChoreDao
    abstract fun completionDao(): CompletionDao

    companion object {
        @Volatile
        private var instance: EssentialsDatabase? = null

        fun getInstance(context: Context): EssentialsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EssentialsDatabase::class.java,
                    "essentials_family.db"
                ).build().also { instance = it }
            }
    }
}

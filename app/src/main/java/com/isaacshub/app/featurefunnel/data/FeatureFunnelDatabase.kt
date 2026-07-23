package com.isaacshub.app.featurefunnel.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FeaturePromptEntity::class], version = 1, exportSchema = true)
abstract class FeatureFunnelDatabase : RoomDatabase() {

    abstract fun featurePromptDao(): FeaturePromptDao

    companion object {
        @Volatile
        private var instance: FeatureFunnelDatabase? = null

        fun getInstance(context: Context): FeatureFunnelDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FeatureFunnelDatabase::class.java,
                    "feature_funnel.db"
                ).build().also { instance = it }
            }
    }
}

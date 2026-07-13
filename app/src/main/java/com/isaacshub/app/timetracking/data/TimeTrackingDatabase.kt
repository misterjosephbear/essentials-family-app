package com.isaacshub.app.timetracking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `deductions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL)"
        )
    }
}

@Database(
    entities = [TimeEntryEntity::class, RouteEntity::class, DeductionEntity::class],
    version = 4,
    exportSchema = true
)
abstract class TimeTrackingDatabase : RoomDatabase() {

    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun routeDao(): RouteDao
    abstract fun deductionDao(): DeductionDao

    companion object {
        @Volatile private var instance: TimeTrackingDatabase? = null

        fun getInstance(context: Context): TimeTrackingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TimeTrackingDatabase::class.java,
                    "time_tracking.db"
                )
                    // Any future schema change must ship an explicit Migration - routes and logged
                    // hours are real user data now and must never be dropped on upgrade.
                    .addMigrations(MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}

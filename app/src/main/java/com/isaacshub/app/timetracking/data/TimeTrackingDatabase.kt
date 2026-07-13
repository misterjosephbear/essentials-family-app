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

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `routes` ADD COLUMN `notes` TEXT")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `route_schedule_overrides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`routeId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [TimeEntryEntity::class, RouteEntity::class, DeductionEntity::class, RouteScheduleOverrideEntity::class],
    version = 6,
    exportSchema = true
)
abstract class TimeTrackingDatabase : RoomDatabase() {

    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun routeDao(): RouteDao
    abstract fun deductionDao(): DeductionDao
    abstract fun routeScheduleOverrideDao(): RouteScheduleOverrideDao

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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { instance = it }
            }
    }
}

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

private fun insertDefaultDeductions(db: SupportSQLiteDatabase) {
    // Percentage-based deductions
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('Social Security', 'PERCENT', 6.2)")
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('Medicare', 'PERCENT', 1.45)")
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('Federal Tax: M 00', 'PERCENT', 7.09)")
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('State Income Tax: IN M 00', 'PERCENT', 2.95)")

    // Flat-rate deductions
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('USPS HB Pln After-tax: (Self only) 200', 'FLAT', 84.00)")
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('USPS HB Pln After-tax: (Self + 1) 200', 'FLAT', 34.75)")
    db.execSQL("INSERT INTO deductions (name, type, amount) VALUES ('Allotment', 'FLAT', 1000.00)")
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add default USPS deductions based on pay stub analysis
        // Only add if deductions table is empty to avoid duplicates
        val cursor = db.query("SELECT COUNT(*) FROM deductions")
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()

        if (count == 0) {
            insertDefaultDeductions(db)
        }
    }
}

@Database(
    entities = [TimeEntryEntity::class, RouteEntity::class, DeductionEntity::class, RouteScheduleOverrideEntity::class],
    version = 7,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default USPS deductions on fresh install
                            insertDefaultDeductions(db)
                        }
                    })
                    .build().also { instance = it }
            }
    }
}

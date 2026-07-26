package com.isaacshub.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.isaacshub.app.routehelper.data.CachedRoadRouteEntity
import com.isaacshub.app.routehelper.data.CandidateAddressEntity
import com.isaacshub.app.routehelper.data.PackageEntity
import com.isaacshub.app.routehelper.data.RouteHelperDao
import com.isaacshub.app.routehelper.data.RouteHelperRouteEntity
import com.isaacshub.app.routehelper.data.RouteSectionEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity
import com.isaacshub.app.timetracking.data.DeductionDao
import com.isaacshub.app.timetracking.data.DeductionEntity
import com.isaacshub.app.timetracking.data.RouteDao
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.RouteScheduleOverrideDao
import com.isaacshub.app.timetracking.data.RouteScheduleOverrideEntity
import com.isaacshub.app.timetracking.data.TimeEntryDao
import com.isaacshub.app.timetracking.data.TimeEntryEntity

/**
 * Consolidated database for work-related features.
 * Combines TimeTracking and RouteHelper databases.
 *
 * Version history:
 * - v1: Initial consolidated database with all entities from both sources
 */
@Database(
    entities = [
        // TimeTracking entities
        TimeEntryEntity::class,
        RouteEntity::class,
        DeductionEntity::class,
        RouteScheduleOverrideEntity::class,
        // RouteHelper entities
        RouteHelperRouteEntity::class,
        CandidateAddressEntity::class,
        RoutedStopEntity::class,
        CachedRoadRouteEntity::class,
        PackageEntity::class,
        RouteSectionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WorkDatabase : RoomDatabase() {

    // TimeTracking DAOs
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun routeDao(): RouteDao
    abstract fun deductionDao(): DeductionDao
    abstract fun routeScheduleOverrideDao(): RouteScheduleOverrideDao

    // RouteHelper DAOs
    abstract fun routeHelperDao(): RouteHelperDao

    companion object {
        @Volatile private var instance: WorkDatabase? = null

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

        fun getInstance(context: Context): WorkDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkDatabase::class.java,
                    "work.db"
                )
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

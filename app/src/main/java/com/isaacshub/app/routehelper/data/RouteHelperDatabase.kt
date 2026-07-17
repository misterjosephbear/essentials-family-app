package com.isaacshub.app.routehelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `routed_stops` ADD COLUMN `recipientLastName` TEXT")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cached_road_routes` (
                `routeId` INTEGER NOT NULL,
                `polylineJson` TEXT NOT NULL,
                `fetchedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`routeId`)
            )
        """.trimIndent())
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `packages` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `routeId` INTEGER NOT NULL,
                `trackingNumber` TEXT NOT NULL,
                `addressLabel` TEXT NOT NULL,
                `routedStopId` INTEGER,
                `isDelivered` INTEGER NOT NULL DEFAULT 0,
                `scannedAtEpochMillis` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `route_sections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `routeId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `startStopId` INTEGER NOT NULL,
                `endStopId` INTEGER NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

@Database(
    entities = [RouteHelperRouteEntity::class, CandidateAddressEntity::class, RoutedStopEntity::class, CachedRoadRouteEntity::class, PackageEntity::class, RouteSectionEntity::class],
    version = 5,
    exportSchema = true
)
abstract class RouteHelperDatabase : RoomDatabase() {

    abstract fun routeHelperDao(): RouteHelperDao

    companion object {
        @Volatile private var instance: RouteHelperDatabase? = null

        fun getInstance(context: Context): RouteHelperDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RouteHelperDatabase::class.java,
                    "route_helper.db"
                )
                    // Same rule as every other database in this app: routes/stops are real user
                    // data - any future schema change needs an explicit Migration, never a
                    // destructive fallback.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { instance = it }
            }
    }
}

package com.isaacshub.app.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.isaacshub.app.banking.data.BankingDatabase
import com.isaacshub.app.featurefunnel.data.FeatureFunnelDatabase
import com.isaacshub.app.routehelper.data.RouteHelperDatabase
import com.isaacshub.app.sleep.data.SleepDatabase
import com.isaacshub.app.timetracking.data.TimeTrackingDatabase

/**
 * Helper to migrate data from old separate databases to new consolidated databases.
 * This preserves all user data during the transition.
 */
object DatabaseMigrationHelper {

    /**
     * Migrates data from SleepDatabase, BankingDatabase, and FeatureFunnelDatabase
     * into the new AppDatabase.
     */
    fun migrateToAppDatabase(context: Context) {
        val oldSleepDb = context.getDatabasePath("sleep.db")
        val oldBankingDb = context.getDatabasePath("banking.db")
        val oldFeatureFunnelDb = context.getDatabasePath("feature_funnel.db")

        if (!oldSleepDb.exists() && !oldBankingDb.exists() && !oldFeatureFunnelDb.exists()) {
            // No old databases to migrate from
            return
        }

        // Create new database (which creates the schema)
        val appDb = AppDatabase.getInstance(context)

        // Attach old databases and copy data
        appDb.openHelper.writableDatabase.apply {
            // Migrate Sleep data
            if (oldSleepDb.exists()) {
                try {
                    execSQL("ATTACH DATABASE '${oldSleepDb.absolutePath}' AS old_sleep")
                    execSQL("INSERT OR REPLACE INTO sleep_sessions SELECT * FROM old_sleep.sleep_sessions")
                    execSQL("DETACH DATABASE old_sleep")
                    oldSleepDb.delete()
                } catch (e: Exception) {
                    // Ignore migration errors for individual databases
                    try { execSQL("DETACH DATABASE old_sleep") } catch (ignored: Exception) {}
                }
            }

            // Migrate Banking data
            if (oldBankingDb.exists()) {
                try {
                    execSQL("ATTACH DATABASE '${oldBankingDb.absolutePath}' AS old_banking")
                    execSQL("INSERT OR REPLACE INTO bank_connections SELECT * FROM old_banking.bank_connections")
                    execSQL("INSERT OR REPLACE INTO bank_accounts SELECT * FROM old_banking.bank_accounts")
                    execSQL("DETACH DATABASE old_banking")
                    oldBankingDb.delete()
                } catch (e: Exception) {
                    try { execSQL("DETACH DATABASE old_banking") } catch (ignored: Exception) {}
                }
            }

            // Migrate FeatureFunnel data
            if (oldFeatureFunnelDb.exists()) {
                try {
                    execSQL("ATTACH DATABASE '${oldFeatureFunnelDb.absolutePath}' AS old_funnel")
                    execSQL("INSERT OR REPLACE INTO feature_prompts SELECT * FROM old_funnel.feature_prompts")
                    execSQL("DETACH DATABASE old_funnel")
                    oldFeatureFunnelDb.delete()
                } catch (e: Exception) {
                    try { execSQL("DETACH DATABASE old_funnel") } catch (ignored: Exception) {}
                }
            }
        }
    }

    /**
     * Migrates data from TimeTrackingDatabase and RouteHelperDatabase
     * into the new WorkDatabase.
     */
    fun migrateToWorkDatabase(context: Context) {
        val oldTimeTrackingDb = context.getDatabasePath("time_tracking.db")
        val oldRouteHelperDb = context.getDatabasePath("route_helper.db")

        if (!oldTimeTrackingDb.exists() && !oldRouteHelperDb.exists()) {
            // No old databases to migrate from
            return
        }

        // Create new database (which creates the schema)
        val workDb = WorkDatabase.getInstance(context)

        // Attach old databases and copy data
        workDb.openHelper.writableDatabase.apply {
            // Migrate TimeTracking data
            if (oldTimeTrackingDb.exists()) {
                try {
                    execSQL("ATTACH DATABASE '${oldTimeTrackingDb.absolutePath}' AS old_tracking")
                    execSQL("INSERT OR REPLACE INTO time_entries SELECT * FROM old_tracking.time_entries")
                    execSQL("INSERT OR REPLACE INTO routes SELECT * FROM old_tracking.routes")
                    execSQL("INSERT OR REPLACE INTO deductions SELECT * FROM old_tracking.deductions")
                    execSQL("INSERT OR REPLACE INTO route_schedule_overrides SELECT * FROM old_tracking.route_schedule_overrides")
                    execSQL("DETACH DATABASE old_tracking")
                    oldTimeTrackingDb.delete()
                } catch (e: Exception) {
                    try { execSQL("DETACH DATABASE old_tracking") } catch (ignored: Exception) {}
                }
            }

            // Migrate RouteHelper data
            if (oldRouteHelperDb.exists()) {
                try {
                    execSQL("ATTACH DATABASE '${oldRouteHelperDb.absolutePath}' AS old_helper")
                    execSQL("INSERT OR REPLACE INTO route_helper_routes SELECT * FROM old_helper.route_helper_routes")
                    execSQL("INSERT OR REPLACE INTO candidate_addresses SELECT * FROM old_helper.candidate_addresses")
                    execSQL("INSERT OR REPLACE INTO routed_stops SELECT * FROM old_helper.routed_stops")
                    execSQL("INSERT OR REPLACE INTO cached_road_routes SELECT * FROM old_helper.cached_road_routes")
                    execSQL("INSERT OR REPLACE INTO packages SELECT * FROM old_helper.packages")
                    execSQL("INSERT OR REPLACE INTO route_sections SELECT * FROM old_helper.route_sections")
                    execSQL("DETACH DATABASE old_helper")
                    oldRouteHelperDb.delete()
                } catch (e: Exception) {
                    try { execSQL("DETACH DATABASE old_helper") } catch (ignored: Exception) {}
                }
            }
        }
    }

    /**
     * Check if migration is needed and perform it.
     * Call this once during app startup.
     */
    fun migrateIfNeeded(context: Context) {
        migrateToAppDatabase(context)
        migrateToWorkDatabase(context)
    }
}

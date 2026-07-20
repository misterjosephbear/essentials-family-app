package com.isaacshub.app.routehelper.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules daily cleanup of packages at 1am.
 */
object PackageCleanupScheduler {
    const val WORK_NAME = "package-cleanup-1am"

    /**
     * Schedules the package cleanup to run daily at 1am.
     * The worker runs every day and deletes packages scanned before 1am today.
     */
    fun schedule(context: Context) {
        // Calculate initial delay until next 1am
        val currentTime = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If it's already past 1am today, schedule for 1am tomorrow
            if (timeInMillis <= currentTime.timeInMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelayMinutes = (nextRun.timeInMillis - currentTime.timeInMillis) / (60 * 1000)

        val request = PeriodicWorkRequestBuilder<PackageCleanupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        android.util.Log.d("PackageCleanupScheduler", "Scheduled package cleanup at 1am daily (initial delay: $initialDelayMinutes minutes)")
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

package com.isaacshub.app.routehelper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.isaacshub.app.App
import java.util.Calendar

/**
 * Worker that clears all packages scanned before 1am today.
 * This ensures the lookahead packages are cleared out at 1am each day.
 */
class PackageCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as App
            val repository = app.routeHelperRepository

            // Calculate 1am today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 1)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val oneAmToday = calendar.timeInMillis

            // Delete all packages scanned before 1am today
            val deletedCount = repository.deletePackagesScanBefore(oneAmToday)

            android.util.Log.d("PackageCleanupWorker", "Deleted $deletedCount packages scanned before 1am today")

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("PackageCleanupWorker", "Failed to clean up packages", e)
            Result.retry()
        }
    }
}

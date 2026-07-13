package com.isaacshub.app.vault.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.isaacshub.app.vault.data.VaultPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object PhotoBackupScheduler {
    private const val PERIODIC_WORK_NAME = "photo-backup"
    private const val ONE_TIME_WORK_NAME = "photo-backup-now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .build()

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PhotoBackupWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /** Triggers an immediate sync (e.g. a "Sync now" button), independent of the periodic schedule. */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PhotoBackupWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            androidx.work.ExistingWorkPolicy.KEEP,
            request
        )
    }

    /** Resumes periodic backup on app startup if a pairing already exists (e.g. after a device reboot). */
    fun rescheduleIfPaired(context: Context, prefs: VaultPreferencesRepository) {
        CoroutineScope(Dispatchers.Default).launch {
            if (prefs.connection.first() != null) {
                schedule(context)
            }
        }
    }
}

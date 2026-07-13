package com.isaacshub.app.vault.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
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

object AppDataBackupScheduler {
    private const val PERIODIC_WORK_NAME = "app-data-backup"
    private const val ONE_TIME_WORK_NAME = "app-data-backup-now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .build()

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AppDataBackupWorker>(1, TimeUnit.DAYS)
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

    fun backupNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AppDataBackupWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
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

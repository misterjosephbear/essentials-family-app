package com.isaacshub.app.featurefunnel.worker

import android.content.Context
import androidx.work.*
import com.isaacshub.app.featurefunnel.data.FeatureFunnelPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object FeatureFunnelScheduler {
    const val PERIODIC_WORK_NAME = "feature-funnel-check"
    const val ONE_TIME_WORK_NAME = "feature-funnel-check-now"

    fun schedule(context: Context, intervalMinutes: Long = 30) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<FeatureFunnelWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
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
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
    }

    /**
     * Starts the worker if feature funnel is enabled.
     * Called on app startup.
     */
    fun rescheduleIfEnabled(context: Context, prefs: FeatureFunnelPreferencesRepository) {
        CoroutineScope(Dispatchers.Default).launch {
            val preferences = prefs.preferences.first()
            if (preferences.enabled) {
                schedule(context, preferences.checkIntervalMinutes.toLong())
            }
        }
    }

    /**
     * Triggers an immediate check (one-time work).
     * Useful for testing or manual trigger from UI.
     */
    fun checkNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<FeatureFunnelWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

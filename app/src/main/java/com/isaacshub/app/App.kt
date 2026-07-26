package com.isaacshub.app

import android.app.Application
import com.isaacshub.app.core.data.AppDatabase
import com.isaacshub.app.core.data.DatabaseMigrationHelper
import com.isaacshub.app.core.data.WorkDatabase
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import com.isaacshub.app.essentials.data.EssentialsApiClient
import com.isaacshub.app.essentials.data.EssentialsDatabase
import com.isaacshub.app.essentials.data.EssentialsRepository
import com.isaacshub.app.featurefunnel.data.FeatureFunnelPreferencesRepository
import com.isaacshub.app.featurefunnel.data.FeatureFunnelRepository
import com.isaacshub.app.featurefunnel.worker.FeatureFunnelScheduler
import com.isaacshub.app.routehelper.data.RouteHelperRepository
import com.isaacshub.app.routehelper.network.RouteHelperAddressFetcher
import com.isaacshub.app.sleep.data.SleepRepository
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.vault.backup.AppDataBackupScheduler
import com.isaacshub.app.vault.data.VaultPreferencesRepository
import com.isaacshub.app.vault.work.PhotoBackupScheduler
import com.isaacshub.app.routehelper.work.PackageCleanupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var sleepRepository: SleepRepository
        private set

    lateinit var timeTrackingRepository: TimeTrackingRepository
        private set

    lateinit var vaultPreferencesRepository: VaultPreferencesRepository
        private set

    lateinit var routeHelperRepository: RouteHelperRepository
        private set

    lateinit var essentialsRepository: EssentialsRepository
        private set

    lateinit var featureFunnelRepository: FeatureFunnelRepository
        private set

    lateinit var featureFunnelPreferencesRepository: FeatureFunnelPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(this)

        // Migrate from old separate databases to new consolidated databases
        DatabaseMigrationHelper.migrateIfNeeded(this)

        // AppDatabase - general app features (Sleep, Banking, FeatureFunnel)
        val appDatabase = AppDatabase.getInstance(this)
        sleepRepository = SleepRepository(appDatabase.sleepSessionDao())

        // WorkDatabase - work-related features (TimeTracking, RouteHelper)
        val workDatabase = WorkDatabase.getInstance(this)
        timeTrackingRepository = TimeTrackingRepository(
            workDatabase.timeEntryDao(),
            workDatabase.routeDao(),
            workDatabase.deductionDao(),
            workDatabase.routeScheduleOverrideDao()
        )
        routeHelperRepository = RouteHelperRepository(workDatabase.routeHelperDao(), RouteHelperAddressFetcher(this))

        vaultPreferencesRepository = VaultPreferencesRepository(this)
        PhotoBackupScheduler.rescheduleIfPaired(this, vaultPreferencesRepository)
        AppDataBackupScheduler.rescheduleIfPaired(this, vaultPreferencesRepository)

        // EssentialsDatabase - kept separate (syncs with server)
        val essentialsDatabase = EssentialsDatabase.getInstance(this)
        essentialsRepository = EssentialsRepository(
            essentialsDatabase.choreDao(),
            essentialsDatabase.childAccountDao(),
            essentialsDatabase.choreCompletionDao(),
            apiClient = null  // Will be set later when connection is available
        )

        // Initialize API client when Vault connection is available
        applicationScope.launch {
            vaultPreferencesRepository.connection.collect { vaultConnection ->
                val apiClient = vaultConnection?.let {
                    EssentialsApiClient(
                        baseUrl = it.baseUrl,
                        apiKey = it.apiKey
                    )
                }

                // Recreate repository with new API client
                essentialsRepository = EssentialsRepository(
                    essentialsDatabase.choreDao(),
                    essentialsDatabase.childAccountDao(),
                    essentialsDatabase.choreCompletionDao(),
                    apiClient = apiClient
                )
            }
        }

        // Feature Funnel initialization
        featureFunnelPreferencesRepository = FeatureFunnelPreferencesRepository(this)
        featureFunnelRepository = FeatureFunnelRepository(
            appDatabase.featurePromptDao(),
            featureFunnelPreferencesRepository
        )
        FeatureFunnelScheduler.rescheduleIfEnabled(this, featureFunnelPreferencesRepository)

        // Schedule daily package cleanup at 1am
        PackageCleanupScheduler.schedule(this)
    }
}

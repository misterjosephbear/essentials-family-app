package com.isaacshub.app

import android.app.Application
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import com.isaacshub.app.sleep.data.SleepDatabase
import com.isaacshub.app.sleep.data.SleepRepository
import com.isaacshub.app.timetracking.data.TimeTrackingDatabase
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.vault.backup.AppDataBackupScheduler
import com.isaacshub.app.vault.data.VaultPreferencesRepository
import com.isaacshub.app.vault.work.PhotoBackupScheduler

class App : Application() {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var sleepRepository: SleepRepository
        private set

    lateinit var timeTrackingRepository: TimeTrackingRepository
        private set

    lateinit var vaultPreferencesRepository: VaultPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(this)

        val sleepDatabase = SleepDatabase.getInstance(this)
        sleepRepository = SleepRepository(sleepDatabase.sleepSessionDao())

        val timeTrackingDatabase = TimeTrackingDatabase.getInstance(this)
        timeTrackingRepository = TimeTrackingRepository(
            timeTrackingDatabase.timeEntryDao(),
            timeTrackingDatabase.routeDao(),
            timeTrackingDatabase.deductionDao(),
            timeTrackingDatabase.routeScheduleOverrideDao()
        )

        vaultPreferencesRepository = VaultPreferencesRepository(this)
        PhotoBackupScheduler.rescheduleIfPaired(this, vaultPreferencesRepository)
        AppDataBackupScheduler.rescheduleIfPaired(this, vaultPreferencesRepository)
    }
}

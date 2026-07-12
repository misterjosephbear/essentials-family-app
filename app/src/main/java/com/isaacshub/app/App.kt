package com.isaacshub.app

import android.app.Application
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import com.isaacshub.app.sleep.data.SleepDatabase
import com.isaacshub.app.sleep.data.SleepRepository

class App : Application() {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var sleepRepository: SleepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(this)
        val database = SleepDatabase.getInstance(this)
        sleepRepository = SleepRepository(database.sleepSessionDao())
    }
}

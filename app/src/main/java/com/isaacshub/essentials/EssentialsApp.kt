package com.isaacshub.essentials

import android.app.Application
import com.isaacshub.essentials.data.local.EssentialsDatabase
import com.isaacshub.essentials.data.repository.AuthRepository
import com.isaacshub.essentials.data.repository.EssentialsRepository

class EssentialsApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var essentialsRepository: EssentialsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize repositories
        authRepository = AuthRepository(this)

        val database = EssentialsDatabase.getInstance(this)
        essentialsRepository = EssentialsRepository(
            choreDao = database.choreDao(),
            completionDao = database.completionDao(),
            authRepository = authRepository
        )
    }
}

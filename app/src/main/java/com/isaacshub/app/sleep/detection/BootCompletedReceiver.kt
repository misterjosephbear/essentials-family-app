package com.isaacshub.app.sleep.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.isaacshub.app.App
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as App
        val pendingResult = goAsync()
        try {
            runBlocking {
                val prefs = app.preferencesRepository.userPreferences.first()
                if (prefs.autoDetectEnabled) {
                    SleepDetectionController.start(context)
                }
            }
        } finally {
            pendingResult.finish()
        }
    }
}

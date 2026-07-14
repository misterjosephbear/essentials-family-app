package com.isaacshub.app.sleep.nap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

class NapAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> NapAlarmController.onAlarmFired(context)
            ACTION_STOP -> {
                val pendingResult = goAsync()
                try {
                    runBlocking { NapAlarmController.stopAlarmAndLog(context) }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.isaacshub.app.sleep.nap.ACTION_FIRE"
        const val ACTION_STOP = "com.isaacshub.app.sleep.nap.ACTION_STOP"
    }
}

package com.isaacshub.app.sleep.detection

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object SleepDetectionController {

    fun start(context: Context) {
        val intent = Intent(context, SleepDetectionService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, SleepDetectionService::class.java))
    }
}

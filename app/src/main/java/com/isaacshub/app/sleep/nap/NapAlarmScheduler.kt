package com.isaacshub.app.sleep.nap

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the one-shot alarm that fires when a nap should end. Uses
 * `setExactAndAllowWhileIdle` when the app holds the exact-alarm permission (granted by default
 * below API 31, requestable via Settings on API 31+) so the alarm fires on time even from Doze;
 * otherwise falls back to an inexact `setAndAllowWhileIdle` alarm, which may fire a few minutes
 * late but still wakes the device.
 */
object NapAlarmScheduler {

    private const val REQUEST_CODE = 2001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NapAlarmReceiver::class.java).setAction(NapAlarmReceiver.ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, triggerAtEpochMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        if (canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context))
    }
}

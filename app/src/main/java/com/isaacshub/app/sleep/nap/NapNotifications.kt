package com.isaacshub.app.sleep.nap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.isaacshub.app.MainActivity
import com.isaacshub.app.R

object NapNotifications {

    private const val CHANNEL_ID = "nap_alarm"
    const val NOTIFICATION_ID = 2002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "Nap alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Wakes you up when your nap timer ends"
            // The ringtone/vibration is driven directly by NapAlarmService so it can be looped
            // and stopped precisely - the notification itself stays silent to avoid a second sound.
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildRingingNotification(context: Context): Notification {
        ensureChannel(context)

        val fullScreenIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, NapAlarmReceiver::class.java).setAction(NapAlarmReceiver.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Nap's over")
            .setContentText("Tap Stop when you're up.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "Stop", stopIntent)
            .setContentIntent(fullScreenIntent)
            .build()
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}

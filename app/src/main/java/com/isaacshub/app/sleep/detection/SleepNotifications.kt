package com.isaacshub.app.sleep.detection

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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SleepNotifications {

    private const val ONGOING_CHANNEL_ID = "sleep_tracking_ongoing"
    private const val SUMMARY_CHANNEL_ID = "sleep_tracking_summary"
    private const val SUMMARY_NOTIFICATION_ID = 1002
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(ONGOING_CHANNEL_ID, "Sleep tracking", NotificationManager.IMPORTANCE_MIN)
        )
        manager.createNotificationChannel(
            NotificationChannel(SUMMARY_CHANNEL_ID, "Sleep summaries", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun buildOngoingNotification(context: Context): Notification {
        ensureChannels(context)
        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setContentTitle("Watching for sleep")
            .setContentText("Isaac's Hub is tracking sleep in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openIntent)
            .build()
    }

    fun notifySessionDetected(context: Context, start: Instant, end: Instant) {
        ensureChannels(context)
        val zone = ZoneId.systemDefault()
        val duration = Duration.between(start, end)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val startStr = start.atZone(zone).format(timeFormatter)
        val endStr = end.atZone(zone).format(timeFormatter)

        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, SUMMARY_CHANNEL_ID)
            .setContentTitle("Good morning! ${hours}h ${minutes}m detected")
            .setContentText("$startStr - $endStr. Tap to confirm or edit.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(SUMMARY_NOTIFICATION_ID, notification)
    }
}

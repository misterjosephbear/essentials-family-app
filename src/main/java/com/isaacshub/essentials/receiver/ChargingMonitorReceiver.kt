package com.isaacshub.essentials.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.isaacshub.essentials.MainActivity
import com.isaacshub.essentials.R
import com.isaacshub.essentials.data.local.entities.CompletionStatus
import com.isaacshub.essentials.data.repository.EssentialsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Monitors device charging status and sends notifications if chores are incomplete
 * when charging after 8pm.
 */
class ChargingMonitorReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ChargingMonitor"
        private const val CHANNEL_ID = "essentials_chore_reminder"
        private const val NOTIFICATION_ID = 2001
        private const val EVENING_HOUR = 20 // 8pm
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        Log.d(TAG, "Received action: $action")

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                onChargingStarted(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Check if currently charging after boot
                if (isCharging(context)) {
                    onChargingStarted(context)
                }
            }
        }
    }

    private fun onChargingStarted(context: Context) {
        Log.d(TAG, "Device started charging")

        // Check if it's after 8pm
        val now = LocalTime.now()
        if (now.hour < EVENING_HOUR) {
            Log.d(TAG, "Not evening time yet (${now.hour}:00 < $EVENING_HOUR:00)")
            return
        }

        Log.d(TAG, "Evening time detected - checking chore completion")

        // Check chore completion status
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as com.isaacshub.essentials.EssentialsApp
                val repository = app.essentialsRepository
                val authRepository = app.authRepository

                // Check if logged in
                if (!authRepository.isLoggedIn()) {
                    Log.d(TAG, "User not logged in - skipping check")
                    return@launch
                }

                val today = LocalDate.now()

                // Get all chores and completions for today
                val chores = repository.observeAllChores().first()
                val todaysChores = chores.filter { today.dayOfWeek in it.daysOfWeek }

                if (todaysChores.isEmpty()) {
                    Log.d(TAG, "No chores for today")
                    return@launch
                }

                val completions = repository.observeCompletionsByDate(today).first()
                val completedChoreIds = completions
                    .filter {
                        it.status == CompletionStatus.VERIFIED ||
                        it.status == CompletionStatus.COMPLETED
                    }
                    .map { it.choreId }

                val incompleteChores = todaysChores.filter { it.id !in completedChoreIds }

                if (incompleteChores.isNotEmpty()) {
                    Log.d(TAG, "${incompleteChores.size} chores incomplete - sending notification")
                    sendChoreReminderNotification(context, incompleteChores.size)
                } else {
                    Log.d(TAG, "All chores complete!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking chore completion", e)
            }
        }
    }

    private fun isCharging(context: Context): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.isCharging
    }

    private fun sendChoreReminderNotification(context: Context, incompleteCount: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Chores Incomplete!")
            .setContentText("You have $incompleteCount incomplete chore${if (incompleteCount > 1) "s" else ""}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You're charging after 8pm but still have $incompleteCount incomplete chore${if (incompleteCount > 1) "s" else ""}. Complete them before using your device!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "Notification sent")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chore Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to complete chores when charging after 8pm"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

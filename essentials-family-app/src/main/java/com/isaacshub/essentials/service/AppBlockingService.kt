package com.isaacshub.essentials.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.isaacshub.essentials.MainActivity
import com.isaacshub.essentials.R
import com.isaacshub.essentials.data.repository.EssentialsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Foreground service that blocks the device when chores are incomplete.
 * Shows an overlay that cannot be dismissed until chores are done.
 */
class AppBlockingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isBlocking = false

    companion object {
        private const val TAG = "AppBlockingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "essentials_blocking"

        fun start(context: Context) {
            val intent = Intent(context, AppBlockingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppBlockingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Start monitoring chore completion
        monitorChoreCompletion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        removeOverlay()
        serviceScope.cancel()
    }

    private fun monitorChoreCompletion() {
        serviceScope.launch {
            // Check every minute if all chores are complete
            while (isActive) {
                checkAndBlockIfNeeded()
                delay(60_000) // 1 minute
            }
        }
    }

    private suspend fun checkAndBlockIfNeeded() {
        try {
            val app = application as com.isaacshub.essentials.EssentialsApp
            val repository = app.essentialsRepository
            val today = LocalDate.now()

            // Get all chores and completions for today
            val chores = repository.observeAllChores().first()
            val todaysChores = chores.filter { today.dayOfWeek in it.daysOfWeek }

            val completions = repository.observeCompletionsByDate(today).first()
            val completedChoreIds = completions
                .filter {
                    it.status == com.isaacshub.essentials.data.local.entities.CompletionStatus.VERIFIED ||
                    it.status == com.isaacshub.essentials.data.local.entities.CompletionStatus.COMPLETED ||
                    it.status == com.isaacshub.essentials.data.local.entities.CompletionStatus.PENDING_VERIFICATION
                }
                .map { it.choreId }

            val incompleteChores = todaysChores.filter { it.id !in completedChoreIds }

            if (incompleteChores.isNotEmpty() && !isBlocking) {
                Log.d(TAG, "${incompleteChores.size} chores incomplete - blocking device")
                showOverlay(incompleteChores.size)
            } else if (incompleteChores.isEmpty() && isBlocking) {
                Log.d(TAG, "All chores complete - removing block")
                removeOverlay()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking chores", e)
        }
    }

    private fun showOverlay(incompleteCount: Int) {
        if (isBlocking) return

        try {
            val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
            } else {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
            }

            layoutParams.gravity = Gravity.CENTER

            // Simple overlay view (in production, inflate a proper layout)
            overlayView = View(this).apply {
                setBackgroundColor(0xCC000000.toInt()) // Semi-transparent black
                setOnClickListener {
                    // Open main activity
                    val intent = Intent(this@AppBlockingService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
            }

            windowManager?.addView(overlayView, layoutParams)
            isBlocking = true

            Log.d(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun removeOverlay() {
        if (!isBlocking) return

        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            isBlocking = false

            Log.d(TAG, "Overlay removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chore Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors chore completion status"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Essentials Active")
            .setContentText("Monitoring chore completion")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}

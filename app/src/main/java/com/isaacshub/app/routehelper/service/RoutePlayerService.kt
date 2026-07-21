package com.isaacshub.app.routehelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.isaacshub.app.MainActivity
import com.isaacshub.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps route player active in the background
 * and displays a system overlay bubble with current/next stop info.
 */
class RoutePlayerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "route_player_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.isaacshub.app.routehelper.START_ROUTE_PLAYER"
        const val ACTION_STOP = "com.isaacshub.app.routehelper.STOP_ROUTE_PLAYER"
        const val EXTRA_ROUTE_ID = "route_id"

        // Shared state for updating the overlay from outside the service
        val currentStopText = MutableStateFlow<String?>(null)
        val nextStopText = MutableStateFlow<String?>(null)
        val packageInfo = MutableStateFlow<String?>(null)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val routeId = intent.getLongExtra(EXTRA_ROUTE_ID, -1L)
                startForeground(NOTIFICATION_ID, createNotification())
                showOverlay()
                startObservingState()
            }
            ACTION_STOP -> {
                stopForeground(true)
                removeOverlay()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Route Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows route navigation status"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Route Player Active")
            .setContentText("Tap to return to route")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        // Check if we have permission to draw overlays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the overlay layout
        overlayView = LayoutInflater.from(this).inflate(R.layout.route_player_overlay, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Make the overlay draggable
        overlayView?.setOnTouchListener(OverlayTouchListener(layoutParams, windowManager))
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
        windowManager = null
    }

    private fun startObservingState() {
        serviceScope.launch {
            currentStopText.collect { text ->
                overlayView?.findViewById<TextView>(R.id.currentStopText)?.text = text ?: "Loading..."
            }
        }
        serviceScope.launch {
            nextStopText.collect { text ->
                overlayView?.findViewById<TextView>(R.id.nextStopText)?.text = text ?: ""
            }
        }
        serviceScope.launch {
            packageInfo.collect { text ->
                overlayView?.findViewById<TextView>(R.id.packageInfoText)?.apply {
                    this.text = text ?: ""
                    visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

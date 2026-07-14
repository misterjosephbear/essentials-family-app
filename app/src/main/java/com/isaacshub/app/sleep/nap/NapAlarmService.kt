package com.isaacshub.app.sleep.nap

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/**
 * Foreground service that rings for an ended nap: loops the device's default alarm sound and
 * vibrates in a repeating pattern until told to stop, either from the notification's Stop action
 * or from the in-app Nap screen. Session logging happens in [NapAlarmController], not here - this
 * class only owns the ringing UX.
 */
class NapAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NapNotifications.NOTIFICATION_ID, NapNotifications.buildRingingNotification(this))
        startRinging()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRinging() {
        val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getValidRingtoneUri(this)
        mediaPlayer = alarmUri?.let { uri ->
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@NapAlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
        }

        val pattern = longArrayOf(0, 500, 500)
        val vibrator = vibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopRinging() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
        vibrator()?.cancel()
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, NapAlarmService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NapAlarmService::class.java))
        }
    }
}

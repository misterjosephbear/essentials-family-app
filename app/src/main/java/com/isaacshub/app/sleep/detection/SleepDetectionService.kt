package com.isaacshub.app.sleep.detection

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.isaacshub.app.App
import com.isaacshub.sleep.core.DetectionConfig
import com.isaacshub.sleep.core.DetectionEvent
import com.isaacshub.sleep.core.DetectionPhase
import com.isaacshub.sleep.core.DetectionResult
import com.isaacshub.sleep.core.DetectionSnapshot
import com.isaacshub.sleep.core.SleepDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Foreground service that drives [SleepDetectionEngine] with real screen on/off broadcasts and
 * the significant-motion trigger sensor. Detection state is snapshotted to [SharedPreferences]
 * after every event so a service restart (process death under memory pressure, etc.) resumes
 * mid-night rather than losing the in-progress session.
 *
 * Ticks run on a plain [Handler], not [android.app.AlarmManager]: while the CPU is asleep a tick
 * may fire later than its nominal 60s interval, but every transition here is duration-based
 * (elapsed time between real event timestamps), so a late tick still computes the correct result -
 * it's just delayed, never wrong. A tighter implementation could use
 * `AlarmManager.setExactAndAllowWhileIdle` for closer-to-schedule ticks.
 */
class SleepDetectionService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)

    private lateinit var sensorManager: SensorManager
    private lateinit var prefs: SharedPreferences
    private lateinit var engine: SleepDetectionEngine

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            handleResult(engine.onTick(Instant.now()))
            saveSnapshot()
            tickHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = Instant.now()
            val event = when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> DetectionEvent.ScreenOff(now)
                Intent.ACTION_SCREEN_ON -> DetectionEvent.ScreenOn(now)
                else -> return
            }
            handleResult(engine.onEvent(event))
            saveSnapshot()
        }
    }

    private val motionListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent) {
            handleResult(engine.onEvent(DetectionEvent.SignificantMotion(Instant.now())))
            saveSnapshot()
            rearmMotionSensor()
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        engine = loadEngine()

        startForeground(NOTIFICATION_ID, SleepNotifications.buildOngoingNotification(this))

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
        )

        rearmMotionSensor()
        tickHandler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(screenReceiver) }
        sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let {
            sensorManager.cancelTriggerSensor(motionListener, it)
        }
        tickHandler.removeCallbacks(tickRunnable)
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun rearmMotionSensor() {
        sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let {
            sensorManager.requestTriggerSensor(motionListener, it)
        }
    }

    private fun handleResult(result: DetectionResult) {
        if (result !is DetectionResult.SessionFinalized) return
        val app = application as App
        scope.launch {
            app.sleepRepository.addDetectedSession(result.start, result.end)
            SleepNotifications.notifySessionDetected(this@SleepDetectionService, result.start, result.end)
        }
    }

    private fun loadEngine(): SleepDetectionEngine {
        val phase = prefs.getString(KEY_PHASE, null)?.let { DetectionPhase.valueOf(it) } ?: DetectionPhase.AWAKE
        val snapshot = DetectionSnapshot(
            phase = phase,
            candidateStartEpochMillis = prefs.getLong(KEY_CANDIDATE_START, -1).takeIf { it != -1L },
            sleepStartEpochMillis = prefs.getLong(KEY_SLEEP_START, -1).takeIf { it != -1L },
            wakeCandidateEpochMillis = prefs.getLong(KEY_WAKE_CANDIDATE, -1).takeIf { it != -1L }
        )
        return SleepDetectionEngine.fromSnapshot(DetectionConfig(), snapshot)
    }

    private fun saveSnapshot() {
        val snapshot = engine.snapshot()
        prefs.edit()
            .putString(KEY_PHASE, snapshot.phase.name)
            .putLong(KEY_CANDIDATE_START, snapshot.candidateStartEpochMillis ?: -1)
            .putLong(KEY_SLEEP_START, snapshot.sleepStartEpochMillis ?: -1)
            .putLong(KEY_WAKE_CANDIDATE, snapshot.wakeCandidateEpochMillis ?: -1)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "sleep_detection_state"
        private const val KEY_PHASE = "phase"
        private const val KEY_CANDIDATE_START = "candidate_start"
        private const val KEY_SLEEP_START = "sleep_start"
        private const val KEY_WAKE_CANDIDATE = "wake_candidate"
        private const val TICK_INTERVAL_MS = 60_000L
        const val NOTIFICATION_ID = 1001
    }
}

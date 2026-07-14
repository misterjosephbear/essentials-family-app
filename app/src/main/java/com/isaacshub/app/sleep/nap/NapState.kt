package com.isaacshub.app.sleep.nap

import android.content.Context
import android.content.SharedPreferences

enum class NapPhase { IDLE, NAPPING, RINGING }

data class NapState(
    val phase: NapPhase,
    val startEpochMillis: Long,
    val alarmEpochMillis: Long
)

/**
 * Persists the in-progress nap (if any) to [SharedPreferences] so it survives process death the
 * same way [com.isaacshub.app.sleep.detection.SleepDetectionService] snapshots its own state -
 * the alarm receiver/service run in whatever process Android chooses to wake, and the UI needs to
 * be able to reload the same state on the next launch.
 */
object NapStateStore {
    private const val PREFS_NAME = "nap_alarm_state"
    private const val KEY_PHASE = "phase"
    private const val KEY_START = "start_epoch_millis"
    private const val KEY_ALARM = "alarm_epoch_millis"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(context: Context): NapState? {
        val p = prefs(context)
        val phase = p.getString(KEY_PHASE, null)?.let { runCatching { NapPhase.valueOf(it) }.getOrNull() }
            ?: return null
        return NapState(
            phase = phase,
            startEpochMillis = p.getLong(KEY_START, -1),
            alarmEpochMillis = p.getLong(KEY_ALARM, -1)
        )
    }

    fun write(context: Context, state: NapState) {
        prefs(context).edit()
            .putString(KEY_PHASE, state.phase.name)
            .putLong(KEY_START, state.startEpochMillis)
            .putLong(KEY_ALARM, state.alarmEpochMillis)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

package com.isaacshub.app.sleep.nap

import android.content.Context
import com.isaacshub.app.App
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Single source of truth for starting, cancelling, and stopping a nap - called both from UI
 * (NapViewModel) and from the alarm receiver (notification Stop action / alarm firing), so the
 * two paths can't drift out of sync with each other.
 */
object NapAlarmController {

    fun startNap(context: Context, durationMinutes: Int) {
        val start = Instant.now()
        val alarmAt = start.plus(durationMinutes.toLong(), ChronoUnit.MINUTES)
        NapStateStore.write(context, NapState(NapPhase.NAPPING, start.toEpochMilli(), alarmAt.toEpochMilli()))
        NapAlarmScheduler.schedule(context, alarmAt.toEpochMilli())
    }

    /** Aborts a nap before the alarm rings - nothing is logged, since the nap never completed. */
    fun cancelNap(context: Context) {
        NapAlarmScheduler.cancel(context)
        NapAlarmService.stop(context)
        NapNotifications.cancel(context)
        NapStateStore.clear(context)
    }

    fun onAlarmFired(context: Context) {
        val state = NapStateStore.read(context) ?: return
        NapStateStore.write(context, state.copy(phase = NapPhase.RINGING))
        NapAlarmService.start(context)
    }

    /** Stops the ringing alarm and logs the nap from its start time to now. */
    suspend fun stopAlarmAndLog(context: Context) {
        val state = NapStateStore.read(context) ?: return
        NapAlarmScheduler.cancel(context)
        NapAlarmService.stop(context)
        NapNotifications.cancel(context)

        val start = Instant.ofEpochMilli(state.startEpochMillis)
        val end = Instant.now()
        if (end.isAfter(start)) {
            val app = context.applicationContext as App
            app.sleepRepository.addNapSession(start, end)
        }

        NapStateStore.clear(context)
    }
}

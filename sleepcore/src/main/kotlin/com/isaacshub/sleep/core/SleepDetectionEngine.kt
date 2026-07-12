package com.isaacshub.sleep.core

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

enum class DetectionPhase { AWAKE, CANDIDATE, ASLEEP }

sealed interface DetectionEvent {
    val at: Instant

    data class ScreenOff(override val at: Instant) : DetectionEvent
    data class ScreenOn(override val at: Instant) : DetectionEvent
    data class SignificantMotion(override val at: Instant) : DetectionEvent
}

data class DetectionConfig(
    /** Hour of day (0-23) sleep is allowed to start being tracked. May be later than [nightWindowEndHour]. */
    val nightWindowStartHour: Int = 20,
    /** Hour of day (0-23) after which a new sleep candidate is no longer started. */
    val nightWindowEndHour: Int = 12,
    /** How long the phone must stay off and still before a candidate becomes an actual sleep session. */
    val stillnessConfirmMinutes: Long = 10,
    /** How long screen-on/motion must persist before a sleep session is considered actually over. */
    val wakeConfirmMinutes: Long = 3,
    /** Safety valve: finalize any session that has run this long, even without a clean wake signal. */
    val maxSessionHours: Long = 16
)

sealed interface DetectionResult {
    data class PhaseChanged(val phase: DetectionPhase) : DetectionResult
    data class SessionFinalized(val start: Instant, val end: Instant) : DetectionResult
    data object NoChange : DetectionResult
}

data class DetectionSnapshot(
    val phase: DetectionPhase,
    val candidateStartEpochMillis: Long?,
    val sleepStartEpochMillis: Long?,
    val wakeCandidateEpochMillis: Long?
)

/**
 * Heuristic sleep detector driven purely by phone screen on/off events and an occasional
 * "significant motion" signal - no proprietary sensor fusion, just a defensible approximation:
 * screen goes off at night, phone stays off and still for a while -> asleep; screen comes back
 * on (or motion resumes) and stays that way -> awake. A brief screen check in the middle of the
 * night does not end the session. Entirely pure/deterministic given injected timestamps, so it
 * has no Android dependency and is unit-testable on its own.
 */
class SleepDetectionEngine(
    private val config: DetectionConfig = DetectionConfig(),
    private var phase: DetectionPhase = DetectionPhase.AWAKE,
    private var candidateStart: Instant? = null,
    private var sleepStart: Instant? = null,
    private var wakeCandidate: Instant? = null
) {

    fun currentPhase(): DetectionPhase = phase

    fun isWithinNightWindow(at: Instant, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val hour = at.atZone(zone).hour
        val start = config.nightWindowStartHour
        val end = config.nightWindowEndHour
        return if (start <= end) hour in start until end else hour >= start || hour < end
    }

    fun onEvent(event: DetectionEvent): DetectionResult = when (event) {
        is DetectionEvent.ScreenOff -> onScreenOff(event.at)
        is DetectionEvent.ScreenOn -> onScreenOn(event.at)
        is DetectionEvent.SignificantMotion -> onMotion(event.at)
    }

    /** Call periodically (e.g. every minute) so time-based transitions fire even without a new event. */
    fun onTick(now: Instant): DetectionResult = when (phase) {
        DetectionPhase.CANDIDATE -> {
            val start = candidateStart
            if (start != null && Duration.between(start, now).toMinutes() >= config.stillnessConfirmMinutes) {
                phase = DetectionPhase.ASLEEP
                sleepStart = start
                DetectionResult.PhaseChanged(phase)
            } else {
                DetectionResult.NoChange
            }
        }
        DetectionPhase.ASLEEP -> {
            val start = sleepStart
            when {
                start == null -> DetectionResult.NoChange
                wakeCandidate != null &&
                    Duration.between(wakeCandidate, now).toMinutes() >= config.wakeConfirmMinutes ->
                    finalize(start, wakeCandidate!!)
                Duration.between(start, now).toHours() >= config.maxSessionHours -> finalize(start, now)
                else -> DetectionResult.NoChange
            }
        }
        DetectionPhase.AWAKE -> DetectionResult.NoChange
    }

    private fun onScreenOff(at: Instant): DetectionResult = when (phase) {
        DetectionPhase.AWAKE -> if (isWithinNightWindow(at)) {
            phase = DetectionPhase.CANDIDATE
            candidateStart = at
            DetectionResult.PhaseChanged(phase)
        } else {
            DetectionResult.NoChange
        }
        DetectionPhase.ASLEEP -> {
            wakeCandidate = null
            DetectionResult.NoChange
        }
        DetectionPhase.CANDIDATE -> DetectionResult.NoChange
    }

    private fun onScreenOn(at: Instant): DetectionResult = when (phase) {
        DetectionPhase.CANDIDATE -> {
            phase = DetectionPhase.AWAKE
            candidateStart = null
            DetectionResult.PhaseChanged(phase)
        }
        DetectionPhase.ASLEEP -> {
            wakeCandidate = at
            DetectionResult.NoChange
        }
        DetectionPhase.AWAKE -> DetectionResult.NoChange
    }

    private fun onMotion(at: Instant): DetectionResult = when (phase) {
        DetectionPhase.CANDIDATE -> {
            phase = DetectionPhase.AWAKE
            candidateStart = null
            DetectionResult.PhaseChanged(phase)
        }
        DetectionPhase.ASLEEP -> {
            if (wakeCandidate == null) wakeCandidate = at
            DetectionResult.NoChange
        }
        DetectionPhase.AWAKE -> DetectionResult.NoChange
    }

    private fun finalize(start: Instant, end: Instant): DetectionResult {
        phase = DetectionPhase.AWAKE
        candidateStart = null
        sleepStart = null
        wakeCandidate = null
        return DetectionResult.SessionFinalized(start, end)
    }

    fun snapshot(): DetectionSnapshot = DetectionSnapshot(
        phase = phase,
        candidateStartEpochMillis = candidateStart?.toEpochMilli(),
        sleepStartEpochMillis = sleepStart?.toEpochMilli(),
        wakeCandidateEpochMillis = wakeCandidate?.toEpochMilli()
    )

    companion object {
        fun fromSnapshot(config: DetectionConfig, snapshot: DetectionSnapshot): SleepDetectionEngine =
            SleepDetectionEngine(
                config = config,
                phase = snapshot.phase,
                candidateStart = snapshot.candidateStartEpochMillis?.let(Instant::ofEpochMilli),
                sleepStart = snapshot.sleepStartEpochMillis?.let(Instant::ofEpochMilli),
                wakeCandidate = snapshot.wakeCandidateEpochMillis?.let(Instant::ofEpochMilli)
            )
    }
}

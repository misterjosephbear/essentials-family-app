package com.isaacshub.sleep.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SleepDetectionEngineTest {

    private fun at(hour: Int, minute: Int = 0, day: Int = 12): Instant =
        ZonedDateTime.of(2026, 7, day, hour, minute, 0, 0, ZoneOffset.UTC).toInstant()

    private fun engineWith(config: DetectionConfig) = SleepDetectionEngine(config = config)

    @Test
    fun `screen off at night starts a candidate`() {
        val engine = engineWith(DetectionConfig(zone = ZoneOffset.UTC, nightWindowStartHour = 20, nightWindowEndHour = 12))
        val result = engine.onEvent(DetectionEvent.ScreenOff(at(23)))
        assertEquals(DetectionResult.PhaseChanged(DetectionPhase.CANDIDATE), result)
    }

    @Test
    fun `screen off during the day does not start a candidate`() {
        val engine = engineWith(DetectionConfig(zone = ZoneOffset.UTC, nightWindowStartHour = 20, nightWindowEndHour = 12))
        val result = engine.onEvent(DetectionEvent.ScreenOff(at(15)))
        assertEquals(DetectionResult.NoChange, result)
        assertEquals(DetectionPhase.AWAKE, engine.currentPhase())
    }

    @Test
    fun `screen back on before stillness confirmed cancels the candidate`() {
        val engine = engineWith(
            DetectionConfig(zone = ZoneOffset.UTC, nightWindowStartHour = 20, nightWindowEndHour = 12, stillnessConfirmMinutes = 10)
        )
        engine.onEvent(DetectionEvent.ScreenOff(at(23)))
        val result = engine.onEvent(DetectionEvent.ScreenOn(at(23, 2)))
        assertEquals(DetectionResult.PhaseChanged(DetectionPhase.AWAKE), result)
    }

    @Test
    fun `significant motion while candidate cancels it too`() {
        val engine = engineWith(
            DetectionConfig(zone = ZoneOffset.UTC, nightWindowStartHour = 20, nightWindowEndHour = 12, stillnessConfirmMinutes = 10)
        )
        engine.onEvent(DetectionEvent.ScreenOff(at(23)))
        val result = engine.onEvent(DetectionEvent.SignificantMotion(at(23, 3)))
        assertEquals(DetectionResult.PhaseChanged(DetectionPhase.AWAKE), result)
    }

    @Test
    fun `candidate becomes asleep after stillness window elapses`() {
        val config = DetectionConfig(zone = ZoneOffset.UTC, nightWindowStartHour = 20, nightWindowEndHour = 12, stillnessConfirmMinutes = 10)
        val engine = engineWith(config)
        engine.onEvent(DetectionEvent.ScreenOff(at(23)))
        val tickResult = engine.onTick(at(23, 11))
        assertEquals(DetectionResult.PhaseChanged(DetectionPhase.ASLEEP), tickResult)
    }

    @Test
    fun `full night produces a finalized session after the wake confirm window`() {
        val config = DetectionConfig(
            zone = ZoneOffset.UTC,
            nightWindowStartHour = 20,
            nightWindowEndHour = 12,
            stillnessConfirmMinutes = 10,
            wakeConfirmMinutes = 3
        )
        val engine = engineWith(config)
        engine.onEvent(DetectionEvent.ScreenOff(at(23, 0, day = 12)))
        engine.onTick(at(23, 11, day = 12))
        engine.onEvent(DetectionEvent.ScreenOn(at(7, 0, day = 13)))
        val result = engine.onTick(at(7, 4, day = 13))

        assertTrue(result is DetectionResult.SessionFinalized)
        result as DetectionResult.SessionFinalized
        assertEquals(at(23, 10, day = 12), result.start)
        assertEquals(at(7, 0, day = 13), result.end)
        assertEquals(DetectionPhase.AWAKE, engine.currentPhase())
    }

    @Test
    fun `brief screen check during the night does not end the session`() {
        val config = DetectionConfig(
            zone = ZoneOffset.UTC,
            nightWindowStartHour = 20,
            nightWindowEndHour = 12,
            stillnessConfirmMinutes = 10,
            wakeConfirmMinutes = 5
        )
        val engine = engineWith(config)
        engine.onEvent(DetectionEvent.ScreenOff(at(23, 0, day = 12)))
        engine.onTick(at(23, 11, day = 12))
        engine.onEvent(DetectionEvent.ScreenOn(at(3, 0, day = 13)))
        engine.onEvent(DetectionEvent.ScreenOff(at(3, 1, day = 13)))
        val tickResult = engine.onTick(at(3, 6, day = 13))

        assertEquals(DetectionResult.NoChange, tickResult)
        assertEquals(DetectionPhase.ASLEEP, engine.currentPhase())
    }

    @Test
    fun `runaway session is finalized by the safety cap`() {
        val config = DetectionConfig(
            zone = ZoneOffset.UTC,
            nightWindowStartHour = 20,
            nightWindowEndHour = 12,
            stillnessConfirmMinutes = 10,
            maxSessionHours = 16
        )
        val engine = engineWith(config)
        engine.onEvent(DetectionEvent.ScreenOff(at(20, 0, day = 12)))
        engine.onTick(at(20, 11, day = 12))
        val result = engine.onTick(at(13, 0, day = 13))

        assertTrue(result is DetectionResult.SessionFinalized)
        assertEquals(DetectionPhase.AWAKE, engine.currentPhase())
    }

    @Test
    fun `snapshot round trips through fromSnapshot`() {
        val config = DetectionConfig(zone = ZoneOffset.UTC)
        val engine = engineWith(config)
        engine.onEvent(DetectionEvent.ScreenOff(at(23)))
        val snapshot = engine.snapshot()
        val restored = SleepDetectionEngine.fromSnapshot(config, snapshot)
        assertEquals(engine.currentPhase(), restored.currentPhase())
        assertEquals(
            DetectionResult.PhaseChanged(DetectionPhase.ASLEEP),
            restored.onTick(at(23, 11))
        )
    }
}

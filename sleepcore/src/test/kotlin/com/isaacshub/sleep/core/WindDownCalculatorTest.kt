package com.isaacshub.sleep.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WindDownCalculatorTest {

    @Test
    fun `no debt recommends bedtime exactly one sleep-need before wake`() {
        val result = WindDownCalculator.calculate(
            usualWakeHourOfDay = 7.0,
            neededMinutesPerNight = 480,
            currentDebtMinutes = 0,
            windDownBufferMinutes = 30
        )
        assertEquals(23.0, result.bedtimeHourOfDay, 0.001)
        assertEquals(22.5, result.windDownHourOfDay, 0.001)
        assertEquals(480, result.targetSleepMinutes)
        assertEquals(0, result.debtRecoveryMinutes)
    }

    @Test
    fun `debt within the cap pushes bedtime earlier`() {
        val result = WindDownCalculator.calculate(
            usualWakeHourOfDay = 7.0,
            neededMinutesPerNight = 480,
            currentDebtMinutes = 45,
            windDownBufferMinutes = 30
        )
        assertEquals(45, result.debtRecoveryMinutes)
        assertEquals(525, result.targetSleepMinutes)
        assertEquals(22.25, result.bedtimeHourOfDay, 0.001)
        assertEquals(21.75, result.windDownHourOfDay, 0.001)
    }

    @Test
    fun `debt recovery is capped so bedtime doesn't get absurdly early`() {
        val result = WindDownCalculator.calculate(
            usualWakeHourOfDay = 7.0,
            neededMinutesPerNight = 480,
            currentDebtMinutes = 600,
            windDownBufferMinutes = 30,
            maxDebtRecoveryMinutesPerNight = 60
        )
        assertEquals(60, result.debtRecoveryMinutes)
        assertEquals(540, result.targetSleepMinutes)
    }

    @Test
    fun `wraps past midnight correctly`() {
        val result = WindDownCalculator.calculate(
            usualWakeHourOfDay = 5.0,
            neededMinutesPerNight = 480,
            currentDebtMinutes = 0,
            windDownBufferMinutes = 30
        )
        assertEquals(21.0, result.bedtimeHourOfDay, 0.001)
        assertEquals(20.5, result.windDownHourOfDay, 0.001)
    }

    @Test
    fun `wraps forward past the start of day when wake time is very early`() {
        val result = WindDownCalculator.calculate(
            usualWakeHourOfDay = 0.5,
            neededMinutesPerNight = 480,
            currentDebtMinutes = 0,
            windDownBufferMinutes = 30
        )
        assertEquals(16.5, result.bedtimeHourOfDay, 0.001)
        assertEquals(16.0, result.windDownHourOfDay, 0.001)
    }
}

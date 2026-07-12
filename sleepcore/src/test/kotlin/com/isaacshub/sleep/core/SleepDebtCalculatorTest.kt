package com.isaacshub.sleep.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SleepDebtCalculatorTest {

    private val today = LocalDate.of(2026, 7, 12)

    @Test
    fun `single night deficit produces matching debt`() {
        val entries = listOf(SleepEntry(today, durationMinutes = 420))
        val result = SleepDebtCalculator.calculate(entries, neededMinutesPerNight = 480, windowDays = 14, asOf = today)
        assertEquals(60, result.totalDebtMinutes)
    }

    @Test
    fun `surplus night reduces debt but floors at zero`() {
        val entries = listOf(
            SleepEntry(today.minusDays(1), durationMinutes = 420),
            SleepEntry(today, durationMinutes = 600)
        )
        val result = SleepDebtCalculator.calculate(entries, neededMinutesPerNight = 480, windowDays = 14, asOf = today)
        assertEquals(0, result.totalDebtMinutes)
    }

    @Test
    fun `nights outside the window are ignored`() {
        val entries = listOf(
            SleepEntry(today.minusDays(20), durationMinutes = 0),
            SleepEntry(today, durationMinutes = 480)
        )
        val result = SleepDebtCalculator.calculate(entries, neededMinutesPerNight = 480, windowDays = 14, asOf = today)
        assertEquals(0, result.totalDebtMinutes)
    }

    @Test
    fun `unconfirmed entries are ignored`() {
        val entries = listOf(SleepEntry(today, durationMinutes = 300, confirmed = false))
        val result = SleepDebtCalculator.calculate(entries, neededMinutesPerNight = 480, windowDays = 14, asOf = today)
        assertEquals(0, result.totalDebtMinutes)
    }

    @Test
    fun `multiple entries on the same date are summed`() {
        val entries = listOf(
            SleepEntry(today, durationMinutes = 300),
            SleepEntry(today, durationMinutes = 60)
        )
        val result = SleepDebtCalculator.calculate(entries, neededMinutesPerNight = 480, windowDays = 14, asOf = today)
        assertEquals(120, result.totalDebtMinutes)
    }

    @Test
    fun `debt accumulates across consecutive deficit nights`() {
        val entries = listOf(
            SleepEntry(today.minusDays(2), durationMinutes = 420),
            SleepEntry(today.minusDays(1), durationMinutes = 420),
            SleepEntry(today, durationMinutes = 420)
        )
        val result = SleepDebtCalculator.calculate(entries, neededMinutesPerNight = 480, windowDays = 14, asOf = today)
        assertEquals(180, result.totalDebtMinutes)
    }

    @Test
    fun `formatDebt formats hours and minutes`() {
        assertEquals("1h 5m", SleepDebtCalculator.formatDebt(65))
        assertEquals("2h", SleepDebtCalculator.formatDebt(120))
        assertEquals("45m", SleepDebtCalculator.formatDebt(45))
        assertEquals("0m", SleepDebtCalculator.formatDebt(0))
    }
}

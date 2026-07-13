package com.isaacshub.app.timetracking.domain

import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val ZONE = ZoneOffset.UTC

/** A HOURLY entry on [date] worked for exactly [hours] (in at 8am, out [hours] later), for carryover math. */
private fun hourlyEntry(id: Long, date: LocalDate, hours: Double): TimeEntryEntity {
    val start = date.atTime(LocalTime.of(8, 0)).atZone(ZONE).toInstant()
    val end = start.plusSeconds((hours * 3600).toLong())
    return TimeEntryEntity(
        id = id,
        startEpochMillis = start.toEpochMilli(),
        endEpochMillis = end.toEpochMilli(),
        payType = PayType.HOURLY,
        routeId = null,
        label = "Test",
        evaluatedHours = null,
        evaluatedMiles = null,
        notes = null,
        createdAtEpochMillis = 0
    )
}

class WeeklySummaryTest {

    // Weeks run Sat-Fri, so these anchor dates are all Saturdays.
    private val week1 = LocalDate.of(2026, 7, 4)
    private val week2 = LocalDate.of(2026, 7, 11)
    private val week3 = LocalDate.of(2026, 7, 18)

    @Test
    fun `undershooting a week raises next week's target above 40`() {
        val entries = listOf(hourlyEntry(1, week1, 30.0))
        val summary = computeWeeklySummary(entries, today = week2, zone = ZONE)
        assertEquals(-10.0, summary.carryoverHours, 0.0001)
        assertEquals(50.0, summary.targetHours, 0.0001)
    }

    @Test
    fun `overshooting a week lowers next week's target below 40`() {
        val entries = listOf(hourlyEntry(1, week1, 50.0))
        val summary = computeWeeklySummary(entries, today = week2, zone = ZONE)
        assertEquals(10.0, summary.carryoverHours, 0.0001)
        assertEquals(30.0, summary.targetHours, 0.0001)
    }

    @Test
    fun `carryover compounds across multiple weeks and is never erased`() {
        val entries = listOf(
            hourlyEntry(1, week1, 30.0),
            hourlyEntry(2, week2, 35.0)
        )
        val summary = computeWeeklySummary(entries, today = week3, zone = ZONE)
        assertEquals(-15.0, summary.carryoverHours, 0.0001)
        assertEquals(55.0, summary.targetHours, 0.0001)
    }

    @Test
    fun `weeks with no logged entries are skipped rather than counted as a deficit`() {
        // No entries at all in week1, only week2 has data - carryover looking forward from week3
        // should reflect just week2, not a phantom -40 from the untracked week1.
        val entries = listOf(hourlyEntry(1, week2, 40.0))
        val summary = computeWeeklySummary(entries, today = week3, zone = ZONE)
        assertEquals(0.0, summary.carryoverHours, 0.0001)
        assertEquals(40.0, summary.targetHours, 0.0001)
    }

    @Test
    fun `the current week itself is not counted in carryover`() {
        val entries = listOf(hourlyEntry(1, week1, 20.0))
        val summary = computeWeeklySummary(entries, today = week1, zone = ZONE)
        assertEquals(0.0, summary.carryoverHours, 0.0001)
        assertEquals(40.0, summary.targetHours, 0.0001)
    }
}

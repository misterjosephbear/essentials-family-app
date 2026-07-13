package com.isaacshub.app.timetracking.domain

import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.RouteScheduleOverrideEntity
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** USPS FLSA workweek used for the overtime calculation. */
private val WEEK_START_DAY = DayOfWeek.SATURDAY
const val OVERTIME_THRESHOLD_HOURS = 40.0

data class WeeklySummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    /** Hours actually getting paid for: clocked hours for HOURLY entries, fixed evaluated hours for EVALUATION. */
    val paidHours: Double,
    /**
     * [paidHours] plus the evaluated hours of any scheduled route occurrence this week that hasn't been
     * logged yet - i.e. what you're on track to be paid for the week if the rest of the schedule holds.
     * A manual EVALUATION entry for the same route on the same day replaces its scheduled placeholder
     * rather than stacking with it.
     */
    val projectedHours: Double,
    /**
     * Actual clocked hours across every entry regardless of pay type. USPS overtime is driven by this number,
     * not by evaluated time - working 39 real hours on a route evaluated at 30 doesn't trigger overtime, but
     * working 40+ real hours does, evaluated or not.
     */
    val actualHours: Double,
    /**
     * Running surplus/deficit against a 40-actual-hour-per-week goal, accumulated over every week logged
     * before this one (never reset). Negative means you're behind and owe extra hours; positive means you
     * banked some slack. This is a personal pacing goal only - it never changes the legal FLSA overtime
     * threshold, which always stays a fixed 40 actual hours in the current week.
     */
    val carryoverHours: Double,
    val entries: List<TimeEntryEntity>
) {
    val overtimeHours: Double get() = (actualHours - OVERTIME_THRESHOLD_HOURS).coerceAtLeast(0.0)
    val isOvertime: Boolean get() = actualHours > OVERTIME_THRESHOLD_HOURS

    /** This week's paced target: 40 hours adjusted by whatever you're ahead or behind from prior weeks. */
    val targetHours: Double get() = OVERTIME_THRESHOLD_HOURS - carryoverHours
}

/** The local calendar date this entry's shift started on. */
fun TimeEntryEntity.localDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startEpochMillis).atZone(zone).toLocalDate()

private fun weekStartFor(date: LocalDate): LocalDate {
    val daysSinceStart = (date.dayOfWeek.value - WEEK_START_DAY.value + 7) % 7
    return date.minusDays(daysSinceStart.toLong())
}

fun currentWeekRange(today: LocalDate = LocalDate.now()): ClosedRange<LocalDate> {
    val start = weekStartFor(today)
    return start..start.plusDays(6)
}

/**
 * Cumulative surplus/deficit (actual hours - 40) across every week before [today]'s week, going back
 * through all recorded history. Weeks with zero logged entries are skipped rather than counted as a
 * full -40 deficit - same "no data means we don't know" rule the sleep debt calculator uses - so this
 * only reflects weeks you actually tracked.
 */
private fun computeCarryoverHours(
    entries: List<TimeEntryEntity>,
    today: LocalDate,
    zone: ZoneId
): Double {
    val currentWeekStart = weekStartFor(today)
    return entries
        .filter { it.localDate(zone).isBefore(currentWeekStart) }
        .groupBy { weekStartFor(it.localDate(zone)) }
        .values
        .sumOf { weekEntries -> weekEntries.sumOf { it.actualHours } - OVERTIME_THRESHOLD_HOURS }
}

fun computeWeeklySummary(
    entries: List<TimeEntryEntity>,
    routes: List<RouteEntity> = emptyList(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    overrides: List<RouteScheduleOverrideEntity> = emptyList()
): WeeklySummary {
    val range = currentWeekRange(today)
    val weekEntries = entries
        .filter { it.localDate(zone) in range }
        .sortedWith(compareByDescending<TimeEntryEntity> { it.startEpochMillis }.thenByDescending { it.id })

    val paidHours = weekEntries.sumOf { entry ->
        when (entry.payType) {
            PayType.HOURLY -> entry.actualHours
            PayType.EVALUATION -> entry.evaluatedHours ?: 0.0
        }
    }
    val actualHours = weekEntries.sumOf { it.actualHours }

    val loggedRouteDates = weekEntries
        .filter { it.payType == PayType.EVALUATION && it.routeId != null }
        .map { it.routeId to it.localDate(zone) }
        .toSet()

    val unloggedScheduledHours = scheduledOccurrences(routes, range, overrides)
        .filter { (route, date) -> (route.id to date) !in loggedRouteDates }
        .sumOf { (route, _) -> route.evaluatedHours }

    return WeeklySummary(
        weekStart = range.start,
        weekEnd = range.endInclusive,
        paidHours = paidHours,
        projectedHours = paidHours + unloggedScheduledHours,
        actualHours = actualHours,
        carryoverHours = computeCarryoverHours(entries, today, zone),
        entries = weekEntries
    )
}

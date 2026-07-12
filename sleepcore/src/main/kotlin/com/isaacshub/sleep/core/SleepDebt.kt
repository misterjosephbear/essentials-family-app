package com.isaacshub.sleep.core

import java.time.LocalDate

/**
 * One night's worth of recorded sleep, attributed to [date] (conventionally the date the
 * sleeper woke up, since a session usually crosses midnight).
 */
data class SleepEntry(
    val date: LocalDate,
    val durationMinutes: Int,
    val confirmed: Boolean = true
)

data class NightlyDebt(
    val date: LocalDate,
    val durationMinutes: Int,
    val neededMinutes: Int,
    val deltaMinutes: Int,
    val runningDebtMinutes: Int
)

data class SleepDebtResult(
    val totalDebtMinutes: Int,
    val nightlyBreakdown: List<NightlyDebt>
)

/**
 * Rolling sleep-debt model: each night short of [neededMinutesPerNight] adds to a running
 * total, each night over it pays the total back down, floored at zero. Only nights within the
 * last [windowDays] (inclusive of [asOf]) count, and only [SleepEntry.confirmed] entries count -
 * nights with no data are simply skipped rather than assumed to be a full deficit.
 */
object SleepDebtCalculator {

    fun calculate(
        entries: List<SleepEntry>,
        neededMinutesPerNight: Int,
        windowDays: Int = 14,
        asOf: LocalDate = LocalDate.now()
    ): SleepDebtResult {
        require(neededMinutesPerNight > 0) { "neededMinutesPerNight must be positive" }
        require(windowDays > 0) { "windowDays must be positive" }

        val cutoff = asOf.minusDays((windowDays - 1).toLong())
        val minutesByDate = entries
            .asSequence()
            .filter { it.confirmed && !it.date.isBefore(cutoff) && !it.date.isAfter(asOf) }
            .groupBy { it.date }
            .mapValues { (_, sameNight) -> sameNight.sumOf { it.durationMinutes } }

        var running = 0
        val breakdown = minutesByDate.keys.sorted().map { date ->
            val duration = minutesByDate.getValue(date)
            val delta = neededMinutesPerNight - duration
            running = (running + delta).coerceAtLeast(0)
            NightlyDebt(date, duration, neededMinutesPerNight, delta, running)
        }

        return SleepDebtResult(running, breakdown)
    }

    fun formatDebt(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> "${mins}m"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }
}

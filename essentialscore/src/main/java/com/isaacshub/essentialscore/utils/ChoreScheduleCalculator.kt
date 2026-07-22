package com.isaacshub.essentialscore.utils

import com.isaacshub.essentialscore.models.Chore
import kotlinx.datetime.*

/**
 * Utility for calculating chore schedules and determining which chores are due on specific dates.
 */
object ChoreScheduleCalculator {

    /**
     * Check if a chore is due on a specific date.
     */
    fun isChoreDueOn(chore: Chore, date: LocalDate): Boolean {
        val dayOfWeek = date.dayOfWeek
        return chore.daysOfWeek.contains(dayOfWeek)
    }

    /**
     * Get all chores that are due on a specific date.
     */
    fun getChoresDueOn(chores: List<Chore>, date: LocalDate): List<Chore> {
        return chores.filter { isChoreDueOn(it, date) }
    }

    /**
     * Get chores due today for the current timezone.
     */
    fun getChoresDueToday(chores: List<Chore>, timeZone: TimeZone = TimeZone.currentSystemDefault()): List<Chore> {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        return getChoresDueOn(chores, today)
    }

    /**
     * Get the next date when a chore will be due (after today).
     */
    fun getNextDueDate(chore: Chore, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate? {
        if (chore.daysOfWeek.isEmpty()) return null

        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date

        // Check the next 7 days
        for (daysAhead in 1..7) {
            val futureDate = today.plus(daysAhead, DateTimeUnit.DAY)
            if (isChoreDueOn(chore, futureDate)) {
                return futureDate
            }
        }

        return null
    }

    /**
     * Check if chores are past the cutoff time (8pm).
     */
    fun isPastCutoffTime(timeZone: TimeZone = TimeZone.currentSystemDefault(), cutoffHour: Int = 20): Boolean {
        val now = Clock.System.now()
        val localTime = now.toLocalDateTime(timeZone).time
        return localTime.hour >= cutoffHour
    }

    /**
     * Get the date string in ISO 8601 format (YYYY-MM-DD) for today.
     */
    fun getTodayDateString(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        return today.toString()
    }

    /**
     * Check if all chores for a specific date have been completed.
     */
    fun areAllChoresComplete(dueChores: List<Chore>, completedChoreIds: Set<Long>): Boolean {
        if (dueChores.isEmpty()) return true
        return dueChores.all { completedChoreIds.contains(it.id) }
    }

    /**
     * Get incomplete chores for a given list of chores and completed IDs.
     */
    fun getIncompleteChores(dueChores: List<Chore>, completedChoreIds: Set<Long>): List<Chore> {
        return dueChores.filter { !completedChoreIds.contains(it.id) }
    }
}

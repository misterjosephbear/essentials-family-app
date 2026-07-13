package com.isaacshub.app.timetracking.domain

import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.RouteScheduleOverrideEntity
import com.isaacshub.app.timetracking.data.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate

/** Whether [route]'s schedule has it running on [date]. */
fun RouteEntity.occursOn(date: LocalDate): Boolean {
    val anchor = scheduleAnchorEpochDay?.let(LocalDate::ofEpochDay)
    return when (scheduleType) {
        ScheduleType.NONE -> false
        ScheduleType.DAILY_SAT_FRI -> date.dayOfWeek != DayOfWeek.SUNDAY
        ScheduleType.DAILY_MON_FRI -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        ScheduleType.WEEKLY -> anchor != null && date.dayOfWeek == anchor.dayOfWeek
        ScheduleType.BIWEEKLY -> {
            if (anchor == null || date.dayOfWeek != anchor.dayOfWeek) {
                false
            } else {
                val weeksBetween = Math.floorDiv(java.time.temporal.ChronoUnit.DAYS.between(anchor, date), 7L)
                Math.floorMod(weeksBetween, 2L) == 0L
            }
        }
    }
}

/**
 * Every (route, date) pair where [route] is scheduled to run within [range], across all [routes] -
 * combining each route's regular recurring schedule with any one-off [overrides] (e.g. covering
 * someone else's route on a specific day). An override that lands on an already-recurring day for
 * that route isn't double-counted.
 */
fun scheduledOccurrences(
    routes: List<RouteEntity>,
    range: ClosedRange<LocalDate>,
    overrides: List<RouteScheduleOverrideEntity> = emptyList()
): List<Pair<RouteEntity, LocalDate>> {
    val routesById = routes.associateBy { it.id }
    val recurring = mutableListOf<Pair<RouteEntity, LocalDate>>()
    var date = range.start
    while (!date.isAfter(range.endInclusive)) {
        routes.forEach { route -> if (route.occursOn(date)) recurring += route to date }
        date = date.plusDays(1)
    }
    val recurringKeys = recurring.map { (route, d) -> route.id to d }.toSet()
    val oneOff = overrides.mapNotNull { override ->
        val overrideDate = LocalDate.ofEpochDay(override.epochDay)
        if (overrideDate !in range) return@mapNotNull null
        val route = routesById[override.routeId] ?: return@mapNotNull null
        if ((route.id to overrideDate) in recurringKeys) return@mapNotNull null
        route to overrideDate
    }
    return recurring + oneOff
}

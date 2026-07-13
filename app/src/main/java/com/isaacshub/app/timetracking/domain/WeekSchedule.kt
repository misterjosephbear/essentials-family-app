package com.isaacshub.app.timetracking.domain

import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import java.time.LocalDate
import java.time.ZoneId

data class ScheduledRouteStatus(
    val route: RouteEntity,
    /** The matching logged EVALUATION entry for this route on this day, if any. */
    val loggedEntry: TimeEntryEntity?
) {
    val isLogged: Boolean get() = loggedEntry != null
}

data class ScheduledDay(
    val date: LocalDate,
    val routes: List<ScheduledRouteStatus>
)

/** Every day of the current week with whichever routes are scheduled to run, and whether each has been logged yet. */
fun computeWeekSchedule(
    entries: List<TimeEntryEntity>,
    routes: List<RouteEntity>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): List<ScheduledDay> {
    val range = currentWeekRange(today)
    val loggedByRouteDate = entries
        .filter { it.payType == PayType.EVALUATION && it.routeId != null }
        .associateBy { it.routeId to it.localDate(zone) }

    val days = mutableListOf<ScheduledDay>()
    var date = range.start
    while (!date.isAfter(range.endInclusive)) {
        val scheduledToday = routes
            .filter { it.occursOn(date) }
            .map { route -> ScheduledRouteStatus(route, loggedByRouteDate[route.id to date]) }
        days += ScheduledDay(date, scheduledToday)
        date = date.plusDays(1)
    }
    return days
}

package com.isaacshub.app.timetracking.domain

import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.RouteScheduleOverrideEntity
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import java.time.LocalDate
import java.time.ZoneId

data class ScheduledRouteStatus(
    val route: RouteEntity,
    /** The matching logged EVALUATION entry for this route on this day, if any. */
    val loggedEntry: TimeEntryEntity?,
    /** Set to the override's id if this occurrence comes from a one-off schedule addition rather than a recurring one. */
    val overrideId: Long? = null
) {
    val isLogged: Boolean get() = loggedEntry != null
    val isOneOff: Boolean get() = overrideId != null
}

data class ScheduledDay(
    val date: LocalDate,
    val routes: List<ScheduledRouteStatus>
)

/**
 * Every day of the current week with whichever routes are scheduled to run - recurring schedule
 * plus any one-off [overrides] - and whether each has been logged yet.
 */
fun computeWeekSchedule(
    entries: List<TimeEntryEntity>,
    routes: List<RouteEntity>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    overrides: List<RouteScheduleOverrideEntity> = emptyList()
): List<ScheduledDay> {
    val range = currentWeekRange(today)
    val loggedByRouteDate = entries
        .filter { it.payType == PayType.EVALUATION && it.routeId != null }
        .associateBy { it.routeId to it.localDate(zone) }
    val routesById = routes.associateBy { it.id }
    val overridesByDate = overrides.groupBy { LocalDate.ofEpochDay(it.epochDay) }

    val days = mutableListOf<ScheduledDay>()
    var date = range.start
    while (!date.isAfter(range.endInclusive)) {
        val recurringRoutes = routes.filter { it.occursOn(date) }
        val recurringIds = recurringRoutes.map { it.id }.toSet()
        val oneOffStatuses = overridesByDate[date].orEmpty()
            .filter { it.routeId !in recurringIds }
            .mapNotNull { override -> routesById[override.routeId]?.let { route -> override to route } }
            .map { (override, route) ->
                ScheduledRouteStatus(route, loggedByRouteDate[route.id to date], overrideId = override.id)
            }
        val recurringStatuses = recurringRoutes.map { route ->
            ScheduledRouteStatus(route, loggedByRouteDate[route.id to date])
        }
        days += ScheduledDay(date, recurringStatuses + oneOffStatuses)
        date = date.plusDays(1)
    }
    return days
}

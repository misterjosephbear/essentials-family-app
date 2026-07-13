package com.isaacshub.app.timetracking.domain

import com.isaacshub.app.timetracking.data.DeductionEntity
import com.isaacshub.app.timetracking.data.DeductionType
import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import java.time.LocalDate
import java.time.ZoneId

data class PayPeriodSummary(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val regularPay: Double,
    val overtimePay: Double,
    val mileagePay: Double,
    val projectedMiles: Double,
    val flatDeductions: Double,
    val percentDeductions: Double
) {
    val grossPay: Double get() = regularPay + overtimePay + mileagePay
    val totalDeductions: Double get() = flatDeductions + percentDeductions

    /** Projected pay this period, net of deductions. */
    val totalPay: Double get() = grossPay - totalDeductions
}

/**
 * Projects pay for the USPS pay period containing [today]: already-logged entries count as-is, and any
 * scheduled route occurrence later in the period that hasn't been logged yet is assumed to run for its
 * evaluated hours/miles. Overtime is figured separately per FLSA workweek (the period is always exactly
 * two Sat-Fri weeks), since that's the actual legal threshold, not the 14-day period as a whole.
 *
 * [deductions] are applied against the gross pay for the whole period: flat deductions subtract their
 * dollar amount directly, percentage deductions subtract that percent of gross pay.
 */
fun computePayPeriodSummary(
    entries: List<TimeEntryEntity>,
    routes: List<RouteEntity>,
    hourlyRate: Double,
    overtimeMultiplier: Double,
    emaRate: Double,
    deductions: List<DeductionEntity> = emptyList(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): PayPeriodSummary {
    val periodRange = currentPayPeriodRange(today)
    val week1Range = periodRange.start..periodRange.start.plusDays(6)
    val week2Range = periodRange.start.plusDays(7)..periodRange.endInclusive

    var regularPay = 0.0
    var overtimePay = 0.0
    var mileagePay = 0.0
    var projectedMiles = 0.0

    for (weekRange in listOf(week1Range, week2Range)) {
        val weekEntries = entries.filter { it.localDate(zone) in weekRange }

        val paidHours = weekEntries.sumOf { entry ->
            when (entry.payType) {
                PayType.HOURLY -> entry.actualHours
                PayType.EVALUATION -> entry.evaluatedHours ?: 0.0
            }
        }
        val actualHours = weekEntries.sumOf { it.actualHours }
        val loggedMiles = weekEntries.sumOf { it.evaluatedMiles ?: 0.0 }

        val loggedRouteDates = weekEntries
            .filter { it.payType == PayType.EVALUATION && it.routeId != null }
            .map { it.routeId to it.localDate(zone) }
            .toSet()
        val unloggedOccurrences = scheduledOccurrences(routes, weekRange)
            .filter { (route, date) -> (route.id to date) !in loggedRouteDates }

        val projectedHours = paidHours + unloggedOccurrences.sumOf { (route, _) -> route.evaluatedHours }
        val projectedActualHours = actualHours + unloggedOccurrences.sumOf { (route, _) -> route.evaluatedHours }
        val weekMiles = loggedMiles + unloggedOccurrences.sumOf { (route, _) -> route.evaluatedMiles }

        val overtimeHours = (projectedActualHours - OVERTIME_THRESHOLD_HOURS).coerceAtLeast(0.0)

        regularPay += projectedHours * hourlyRate
        overtimePay += overtimeHours * hourlyRate * (overtimeMultiplier - 1.0)
        mileagePay += weekMiles * emaRate
        projectedMiles += weekMiles
    }

    val grossPay = regularPay + overtimePay + mileagePay
    val flatDeductions = deductions.filter { it.type == DeductionType.FLAT }.sumOf { it.amount }
    val percentDeductions = deductions
        .filter { it.type == DeductionType.PERCENT }
        .sumOf { grossPay * it.amount / 100.0 }

    return PayPeriodSummary(
        periodStart = periodRange.start,
        periodEnd = periodRange.endInclusive,
        regularPay = regularPay,
        overtimePay = overtimePay,
        mileagePay = mileagePay,
        projectedMiles = projectedMiles,
        flatDeductions = flatDeductions,
        percentDeductions = percentDeductions
    )
}

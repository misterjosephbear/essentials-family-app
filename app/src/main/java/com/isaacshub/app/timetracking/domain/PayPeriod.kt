package com.isaacshub.app.timetracking.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Start of USPS pay period PP02-2026 (Sat, Dec 27, 2025), per the officially published 2026 pay period
 * calendar. Every pay period is a fixed 14-day Saturday-Friday cycle, so any other period's boundaries can
 * be derived from this single anchor. Double-check this against a pay stub if the shown period looks off.
 */
private val PAY_PERIOD_ANCHOR: LocalDate = LocalDate.of(2025, 12, 27)
private const val PAY_PERIOD_DAYS = 14L

fun currentPayPeriodRange(today: LocalDate = LocalDate.now()): ClosedRange<LocalDate> {
    val daysSinceAnchor = ChronoUnit.DAYS.between(PAY_PERIOD_ANCHOR, today)
    val periodIndex = Math.floorDiv(daysSinceAnchor, PAY_PERIOD_DAYS)
    val start = PAY_PERIOD_ANCHOR.plusDays(periodIndex * PAY_PERIOD_DAYS)
    return start..start.plusDays(PAY_PERIOD_DAYS - 1)
}

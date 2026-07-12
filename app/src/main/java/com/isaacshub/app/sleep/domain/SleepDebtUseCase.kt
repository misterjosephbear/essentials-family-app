package com.isaacshub.app.sleep.domain

import com.isaacshub.app.core.data.prefs.UserPreferences
import com.isaacshub.app.sleep.data.SleepSessionEntity
import com.isaacshub.app.sleep.data.toSleepEntry
import com.isaacshub.sleep.core.SleepDebtCalculator
import com.isaacshub.sleep.core.SleepDebtResult
import java.time.LocalDate

fun computeDebt(
    sessions: List<SleepSessionEntity>,
    prefs: UserPreferences,
    asOf: LocalDate = LocalDate.now()
): SleepDebtResult {
    val entries = sessions.map { it.toSleepEntry() }
    return SleepDebtCalculator.calculate(
        entries = entries,
        neededMinutesPerNight = prefs.sleepNeedMinutes,
        windowDays = prefs.debtWindowDays,
        asOf = asOf
    )
}

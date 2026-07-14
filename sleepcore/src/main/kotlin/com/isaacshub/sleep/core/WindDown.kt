package com.isaacshub.sleep.core

data class WindDownRecommendation(
    val windDownHourOfDay: Double,
    val bedtimeHourOfDay: Double,
    val targetSleepMinutes: Int,
    val debtRecoveryMinutes: Int
)

/**
 * Recommends when to start winding down for bed, working backward from a usual wake time:
 * bedtime = wake time - (sleep need + a bite of debt repayment), wind-down start = bedtime minus
 * a buffer to settle in before lights-out. Debt repayment is capped per night so a large debt
 * doesn't recommend an unreasonably early bedtime - whatever isn't repaid tonight simply carries
 * into tomorrow's debt calculation, same as any other short night.
 */
object WindDownCalculator {

    fun calculate(
        usualWakeHourOfDay: Double,
        neededMinutesPerNight: Int,
        currentDebtMinutes: Int,
        windDownBufferMinutes: Int = 30,
        maxDebtRecoveryMinutesPerNight: Int = 60
    ): WindDownRecommendation {
        require(neededMinutesPerNight > 0) { "neededMinutesPerNight must be positive" }
        require(windDownBufferMinutes >= 0) { "windDownBufferMinutes must not be negative" }
        require(maxDebtRecoveryMinutesPerNight >= 0) { "maxDebtRecoveryMinutesPerNight must not be negative" }

        val debtRecovery = currentDebtMinutes.coerceIn(0, maxDebtRecoveryMinutesPerNight)
        val targetSleepMinutes = neededMinutesPerNight + debtRecovery
        val bedtimeHourOfDay = wrapHours(usualWakeHourOfDay - targetSleepMinutes / 60.0)
        val windDownHourOfDay = wrapHours(bedtimeHourOfDay - windDownBufferMinutes / 60.0)

        return WindDownRecommendation(
            windDownHourOfDay = windDownHourOfDay,
            bedtimeHourOfDay = bedtimeHourOfDay,
            targetSleepMinutes = targetSleepMinutes,
            debtRecoveryMinutes = debtRecovery
        )
    }

    private fun wrapHours(hours: Double): Double = ((hours % 24.0) + 24.0) % 24.0
}

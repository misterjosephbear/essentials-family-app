package com.isaacshub.sleep.core

import kotlin.math.cos
import kotlin.math.exp

enum class EnergyLabelType { PEAK, DIP }

data class EnergyPoint(
    val hoursAwake: Double,
    val hourOfDay: Double,
    val energy: Double
)

data class EnergyLabel(
    val hoursAwake: Double,
    val hourOfDay: Double,
    val energy: Double,
    val type: EnergyLabelType
)

data class EnergyForecast(
    val points: List<EnergyPoint>,
    val labels: List<EnergyLabel>
)

/**
 * Rough estimate of energy/alertness across the day, styled after the two-process model of sleep
 * regulation (Borbély): a circadian rhythm (a primary ~24h cycle plus a smaller ~12h harmonic that
 * produces the well-documented post-lunch dip and early-evening rebound) combined with homeostatic
 * sleep pressure that builds the longer you've been awake. Existing sleep debt raises that pressure,
 * which compresses the whole curve toward its low end rather than moving where the peaks/dips land -
 * this is a stylized approximation for a daily-planning visual, not a clinical or validated model.
 */
object EnergyForecastCalculator {

    private const val PRIMARY_AMPLITUDE = 0.6
    private const val PRIMARY_PEAK_HOUR = 10.0
    private const val SECONDARY_AMPLITUDE = 0.35
    private const val SECONDARY_PEAK_HOUR = 8.0
    private const val PRESSURE_TIME_CONSTANT_HOURS = 8.0
    private const val PRESSURE_WEIGHT = 0.55
    private const val DEBT_SCALE_MINUTES = 480.0
    private const val MAX_DEBT_BONUS = 0.6

    // Fixed rescale window (not a per-day min/max) so higher sleep debt visibly compresses the
    // curve instead of being renormalized away.
    private const val SCALE_MIN = -2.0
    private const val SCALE_MAX = 0.95

    fun calculate(
        wakeHourOfDay: Double,
        debtMinutes: Int,
        awakeHours: Double = 17.0,
        stepMinutes: Int = 15
    ): EnergyForecast {
        require(awakeHours > 0) { "awakeHours must be positive" }
        require(stepMinutes > 0) { "stepMinutes must be positive" }

        val stepHours = stepMinutes / 60.0
        val steps = (awakeHours / stepHours).toInt()
        val points = (0..steps).map { i ->
            val hoursAwake = (i * stepHours).coerceAtMost(awakeHours)
            val hourOfDay = (wakeHourOfDay + hoursAwake) % 24.0
            val raw = rawEnergy(hourOfDay, hoursAwake, debtMinutes)
            EnergyPoint(hoursAwake, hourOfDay, scale(raw))
        }
        return EnergyForecast(points, findLabels(points))
    }

    private fun rawEnergy(hourOfDay: Double, hoursAwake: Double, debtMinutes: Int): Double {
        val primary = PRIMARY_AMPLITUDE * cos(2 * Math.PI * (hourOfDay - PRIMARY_PEAK_HOUR) / 24.0)
        val secondary = SECONDARY_AMPLITUDE * cos(2 * Math.PI * (hourOfDay - SECONDARY_PEAK_HOUR) / 12.0)
        val circadian = primary + secondary
        val debtFactor = 1.0 + (debtMinutes.coerceAtLeast(0) / DEBT_SCALE_MINUTES) * MAX_DEBT_BONUS
        val pressure = debtFactor * (1 - exp(-hoursAwake / PRESSURE_TIME_CONSTANT_HOURS))
        return circadian - PRESSURE_WEIGHT * pressure
    }

    private fun scale(raw: Double): Double =
        ((raw - SCALE_MIN) / (SCALE_MAX - SCALE_MIN)).coerceIn(0.0, 1.0)

    private fun findLabels(points: List<EnergyPoint>): List<EnergyLabel> {
        val labels = mutableListOf<EnergyLabel>()
        for (i in 1 until points.size - 1) {
            val prev = points[i - 1].energy
            val curr = points[i].energy
            val next = points[i + 1].energy
            when {
                curr > prev && curr > next ->
                    labels += EnergyLabel(points[i].hoursAwake, points[i].hourOfDay, curr, EnergyLabelType.PEAK)
                curr < prev && curr < next ->
                    labels += EnergyLabel(points[i].hoursAwake, points[i].hourOfDay, curr, EnergyLabelType.DIP)
            }
        }
        return labels
    }
}

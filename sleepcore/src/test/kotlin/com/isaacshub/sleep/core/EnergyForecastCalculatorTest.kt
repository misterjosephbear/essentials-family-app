package com.isaacshub.sleep.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyForecastCalculatorTest {

    @Test
    fun `all energy values stay within 0 to 1`() {
        val forecast = EnergyForecastCalculator.calculate(wakeHourOfDay = 6.5, debtMinutes = 300)
        assertTrue(forecast.points.all { it.energy in 0.0..1.0 })
    }

    @Test
    fun `point count matches the requested window and step`() {
        val forecast = EnergyForecastCalculator.calculate(
            wakeHourOfDay = 7.0,
            debtMinutes = 0,
            awakeHours = 16.0,
            stepMinutes = 60
        )
        assertEquals(17, forecast.points.size)
    }

    @Test
    fun `finds a morning peak followed by an afternoon dip`() {
        val forecast = EnergyForecastCalculator.calculate(wakeHourOfDay = 6.5, debtMinutes = 0)
        val peak = forecast.labels.first { it.type == EnergyLabelType.PEAK }
        val dip = forecast.labels.first { it.type == EnergyLabelType.DIP }
        assertTrue("peak should land within a few hours of waking", peak.hoursAwake in 0.5..5.0)
        assertTrue("dip should come after the peak", dip.hoursAwake > peak.hoursAwake)
    }

    @Test
    fun `higher sleep debt lowers energy later in the day without shifting the window`() {
        val rested = EnergyForecastCalculator.calculate(wakeHourOfDay = 6.5, debtMinutes = 0)
        val exhausted = EnergyForecastCalculator.calculate(wakeHourOfDay = 6.5, debtMinutes = 600)
        val restedLateEnergy = rested.points.last { it.hoursAwake <= 10.0 }.energy
        val exhaustedLateEnergy = exhausted.points.last { it.hoursAwake <= 10.0 }.energy
        assertTrue(exhaustedLateEnergy < restedLateEnergy)
    }

    @Test
    fun `wake hour anchors the first point with zero hours awake`() {
        val forecast = EnergyForecastCalculator.calculate(wakeHourOfDay = 6.5, debtMinutes = 0)
        assertEquals(0.0, forecast.points.first().hoursAwake, 0.0001)
        assertEquals(6.5, forecast.points.first().hourOfDay, 0.0001)
    }
}

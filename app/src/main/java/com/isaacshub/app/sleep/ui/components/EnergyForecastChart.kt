package com.isaacshub.app.sleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.isaacshub.sleep.core.EnergyForecast
import com.isaacshub.sleep.core.EnergyLabelType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val hourFormatter = DateTimeFormatter.ofPattern("h a")
private val chartHeight = 440.dp

private fun formatHourOfDay(hourOfDay: Double): String {
    val hour = hourOfDay.toInt() % 24
    val minute = ((hourOfDay - hourOfDay.toInt()) * 60).roundToInt() % 60
    return LocalTime.of(hour, minute).format(hourFormatter)
}

/**
 * Vertical energy timeline, styled after apps like Rise: time flows top-to-bottom and the band's
 * width at each point is how much energy [forecast] estimates for that moment - wide is alert,
 * narrow is a lull.
 */
@Composable
fun EnergyForecastChart(
    forecast: EnergyForecast,
    currentHoursAwake: Double?,
    modifier: Modifier = Modifier
) {
    val fillColor = MaterialTheme.colorScheme.primary
    val nowColor = MaterialTheme.colorScheme.error
    val density = LocalDensity.current
    val points = forecast.points
    if (points.size < 2) return
    val totalHours = points.last().hoursAwake.coerceAtLeast(0.01)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val heightPx = with(density) { maxHeight.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val centerX = widthPx * 0.36f
        val maxHalfWidth = widthPx * 0.24f

        fun yFor(hoursAwake: Double): Float = (hoursAwake / totalHours * heightPx).toFloat()

        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            val leftPts = points.map { Offset(centerX - it.energy.toFloat() * maxHalfWidth, yFor(it.hoursAwake)) }
            val rightPts = points.map { Offset(centerX + it.energy.toFloat() * maxHalfWidth, yFor(it.hoursAwake)) }
            val path = Path().apply {
                moveTo(leftPts.first().x, leftPts.first().y)
                leftPts.drop(1).forEach { lineTo(it.x, it.y) }
                rightPts.asReversed().forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(fillColor.copy(alpha = 0.9f), fillColor.copy(alpha = 0.35f))
                )
            )
        }

        val wakeHour = points.first().hourOfDay
        var tickHours = 0.0
        while (tickHours <= totalHours + 0.01) {
            val hourOfDay = (wakeHour + tickHours) % 24.0
            val y = yFor(tickHours)
            Text(
                text = formatHourOfDay(hourOfDay),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.offset { IntOffset(0, (y - 10.dp.toPx()).roundToInt()) }
            )
            tickHours += 3.0
        }

        forecast.labels.forEach { label ->
            val y = yFor(label.hoursAwake)
            val text = if (label.type == EnergyLabelType.PEAK) {
                "Peak - ${formatHourOfDay(label.hourOfDay)}"
            } else {
                "Dip - ${formatHourOfDay(label.hourOfDay)}"
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.offset {
                    val x = centerX + label.energy.toFloat() * maxHalfWidth + 12.dp.toPx()
                    IntOffset(x.roundToInt(), (y - 10.dp.toPx()).roundToInt())
                }
            )
        }

        if (currentHoursAwake != null && currentHoursAwake in 0.0..totalHours) {
            val y = yFor(currentHoursAwake)
            Box(
                modifier = Modifier
                    .offset {
                        val dotRadius = 6.dp.toPx()
                        IntOffset((centerX - dotRadius).roundToInt(), (y - dotRadius).roundToInt())
                    }
                    .size(12.dp)
                    .background(nowColor, CircleShape)
            )
        }
    }
}

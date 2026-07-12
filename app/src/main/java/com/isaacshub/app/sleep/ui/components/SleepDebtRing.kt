package com.isaacshub.app.sleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.isaacshub.sleep.core.SleepDebtCalculator

@Composable
fun SleepDebtRing(
    debtMinutes: Int,
    capMinutes: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (capMinutes <= 0) 0f else (debtMinutes.toFloat() / capMinutes).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = Offset(stroke.width / 2, stroke.width / 2)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = arcSize,
                topLeft = topLeft
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = stroke,
                size = arcSize,
                topLeft = topLeft
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = SleepDebtCalculator.formatDebt(debtMinutes),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "sleep debt",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

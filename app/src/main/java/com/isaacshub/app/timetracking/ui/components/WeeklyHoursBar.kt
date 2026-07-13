package com.isaacshub.app.timetracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaacshub.app.timetracking.domain.WeeklySummary
import com.isaacshub.app.timetracking.ui.formatNumber

@Composable
fun WeeklyHoursBar(
    summary: WeeklySummary,
    modifier: Modifier = Modifier
) {
    val target = summary.targetHours
    val fraction = (summary.projectedHours / target.takeIf { it > 0.0 }.let { it ?: 0.0001 })
        .toFloat()
        .coerceIn(0f, 1f)
    val progressColor = if (summary.isOvertime) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("This week's projected hours", style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatNumber(summary.projectedHours)} / ${formatNumber(target)} hrs",
                style = MaterialTheme.typography.titleMedium
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Text(
            "Actual hours worked: ${formatNumber(summary.actualHours)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Carryover hours: ${formatNumber(summary.carryoverHours)}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (summary.isOvertime) {
            Text(
                "You're earning overtime - ${formatNumber(summary.overtimeHours)} actual hrs over 40",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

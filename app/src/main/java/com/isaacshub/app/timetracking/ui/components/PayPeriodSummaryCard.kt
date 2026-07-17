package com.isaacshub.app.timetracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaacshub.app.timetracking.domain.PayPeriodSummary
import com.isaacshub.app.timetracking.ui.formatCurrency
import com.isaacshub.app.timetracking.ui.formatNumber
import java.time.format.DateTimeFormatter

private val periodFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun PayPeriodSummaryCard(
    summary: PayPeriodSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Projected pay this period", style = MaterialTheme.typography.titleMedium)
            Text(formatCurrency(summary.totalPay), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "${summary.periodStart.format(periodFormatter)} - ${summary.periodEnd.format(periodFormatter)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "${formatCurrency(summary.mileagePay)} from mileage (${formatNumber(summary.projectedMiles)} mi)",
            style = MaterialTheme.typography.bodyMedium
        )
        if (summary.overtimeHours > 0) {
            Text(
                "Current overtime this pay period: ${formatNumber(summary.overtimeHours)} hrs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (summary.overtimePay > 0) {
            Text(
                "Includes ${formatCurrency(summary.overtimePay)} overtime premium",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (summary.totalDeductions > 0) {
            Text(
                "${formatCurrency(summary.grossPay)} gross - ${formatCurrency(summary.totalDeductions)} deductions",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

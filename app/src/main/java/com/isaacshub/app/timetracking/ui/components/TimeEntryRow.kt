package com.isaacshub.app.timetracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaacshub.app.timetracking.data.PayType
import com.isaacshub.app.timetracking.data.TimeEntryEntity
import com.isaacshub.app.timetracking.ui.formatNumber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val entryDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
fun TimeEntryRow(
    entry: TimeEntryEntity,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val date = Instant.ofEpochMilli(entry.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    val detail = when (entry.payType) {
        PayType.HOURLY -> "${formatNumber(entry.actualHours)}h worked"
        PayType.EVALUATION ->
            "${formatNumber(entry.actualHours)}h worked - ${formatNumber(entry.evaluatedHours ?: 0.0)} eval hrs - " +
                "${formatNumber(entry.evaluatedMiles ?: 0.0)} mi"
    }

    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(entry.label, style = MaterialTheme.typography.titleMedium)
                Text(date.format(entryDateFormatter), style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (entry.payType == PayType.HOURLY) "Hourly" else "Evaluation") }
                    )
                    Text(detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

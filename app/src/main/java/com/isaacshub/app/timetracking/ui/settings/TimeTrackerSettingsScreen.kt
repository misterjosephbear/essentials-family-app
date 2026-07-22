package com.isaacshub.app.timetracking.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.timetracking.data.DeductionEntity
import com.isaacshub.app.timetracking.data.DeductionType
import com.isaacshub.app.timetracking.ui.formatCurrency
import com.isaacshub.app.timetracking.ui.formatNumber

@Composable
fun TimeTrackerSettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: TimeTrackerSettingsViewModel = viewModel(
        factory = TimeTrackerSettingsViewModel.Factory(app.preferencesRepository, app.timeTrackingRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val deductionForm by viewModel.deductionForm.collectAsState()
    val deductions by viewModel.deductions.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Pay rates", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = state.hourlyRate,
                    onValueChange = viewModel::setHourlyRate,
                    label = { Text("Hourly rate ($/hr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.overtimeMultiplier,
                    onValueChange = viewModel::setOvertimeMultiplier,
                    label = { Text("Overtime multiplier (e.g. 1.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.emaRate,
                    onValueChange = viewModel::setEmaRate,
                    label = { Text("EMA rate ($/mile)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.carryoverAdjustment,
                    onValueChange = viewModel::setCarryoverAdjustment,
                    label = { Text("Carryover hours adjustment") },
                    supportingText = { Text("Manually add (+) or subtract (-) from accumulated carryover hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            if (state.justSaved) {
                item { Text("Saved", color = MaterialTheme.colorScheme.primary) }
            }
            item {
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text("Save")
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text("Deductions", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text(
                    "Subtracted from projected pay this period - flat deductions subtract a dollar amount, " +
                        "percent deductions subtract that percent of gross pay.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (deductions.isEmpty()) {
                item { Text("No deductions added yet.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(deductions, key = { it.id }) { deduction ->
                    DeductionRow(deduction = deduction, onDelete = { viewModel.deleteDeduction(deduction) })
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = deductionForm.name,
                            onValueChange = viewModel::setDeductionName,
                            label = { Text("Deduction name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = deductionForm.type == DeductionType.FLAT,
                                onClick = { viewModel.setDeductionType(DeductionType.FLAT) },
                                label = { Text("Flat $") }
                            )
                            FilterChip(
                                selected = deductionForm.type == DeductionType.PERCENT,
                                onClick = { viewModel.setDeductionType(DeductionType.PERCENT) },
                                label = { Text("Percent %") }
                            )
                        }
                        OutlinedTextField(
                            value = deductionForm.amount,
                            onValueChange = viewModel::setDeductionAmount,
                            label = {
                                Text(if (deductionForm.type == DeductionType.FLAT) "Amount ($)" else "Amount (%)")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        deductionForm.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(onClick = viewModel::addDeduction, modifier = Modifier.fillMaxWidth()) {
                            Text("Add deduction")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeductionRow(
    deduction: DeductionEntity,
    onDelete: () -> Unit
) {
    val valueLabel = when (deduction.type) {
        DeductionType.FLAT -> "${formatCurrency(deduction.amount)} flat"
        DeductionType.PERCENT -> "${formatNumber(deduction.amount)}% of gross"
    }
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(deduction.name, style = MaterialTheme.typography.bodyLarge)
                Text(valueLabel, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

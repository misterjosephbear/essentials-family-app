package com.isaacshub.app.banking.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.isaacshub.app.banking.domain.BudgetCategory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CategoryEditDialog(
    category: BudgetCategory,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    var thresholdText by remember { mutableStateOf(category.threshold.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit ${category.name}")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it
                        errorMessage = null
                    },
                    label = { Text("Threshold Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This category will be filled once the balance reaches ${currencyFormatter.format(category.threshold)}.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = thresholdText.toDoubleOrNull()
                    when {
                        value == null -> errorMessage = "Please enter a valid number"
                        value < 0 -> errorMessage = "Amount must be positive"
                        else -> onSave(value)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.isaacshub.app.banking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaacshub.app.banking.domain.BudgetState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetTowerCard(
    budgetState: BudgetState,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Overview",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onConfigureClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Configure Budget")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total Available: ${currencyFormatter.format(budgetState.totalBalance)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Render categories from top to bottom (reversed for display)
            budgetState.categories.asReversed().forEach { categoryState ->
                BudgetCategoryBar(categoryState = categoryState)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

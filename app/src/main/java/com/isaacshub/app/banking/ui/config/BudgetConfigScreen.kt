package com.isaacshub.app.banking.ui.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaacshub.app.banking.domain.BudgetCategory
import com.isaacshub.app.banking.ui.components.AccountSelectionDialog
import com.isaacshub.app.banking.ui.components.BudgetTowerCard
import com.isaacshub.app.banking.ui.components.CategoryEditDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetConfigScreen(
    viewModel: BudgetConfigViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Account Selection Section
                item {
                    Text(
                        text = "Selected Accounts",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (uiState.selectedAccountIds.isEmpty()) {
                                Text(
                                    text = "No accounts selected for budget tracking.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    text = "${uiState.selectedAccountIds.size} account(s) selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.openAccountPicker() }) {
                                Text("Choose Accounts")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Budget Categories Section
                item {
                    Text(
                        text = "Budget Categories",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set threshold amounts for each category. The budget fills from bottom to top.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(uiState.categories) { category ->
                    BudgetCategoryConfigCard(
                        category = category,
                        currencyFormatter = currencyFormatter,
                        onEdit = { viewModel.editCategory(category) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Preview Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    val budgetState = uiState.budgetState
                    if (budgetState != null) {
                        BudgetTowerCard(
                            budgetState = budgetState,
                            onConfigureClick = {}
                        )
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Select accounts to see budget preview",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showAccountPicker) {
            AccountSelectionDialog(
                accounts = uiState.accounts,
                selectedAccountIds = uiState.selectedAccountIds,
                onToggleAccount = { viewModel.toggleAccountSelection(it) },
                onDismiss = { viewModel.closeAccountPicker() }
            )
        }

        uiState.editingCategory?.let { category ->
            CategoryEditDialog(
                category = category,
                onSave = { newThreshold ->
                    viewModel.saveCategoryThreshold(category.id, newThreshold)
                },
                onDismiss = { viewModel.closeEditDialog() }
            )
        }
    }
}

@Composable
private fun BudgetCategoryConfigCard(
    category: BudgetCategory,
    currencyFormatter: NumberFormat,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Threshold: ${currencyFormatter.format(category.threshold)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${category.name}")
            }
        }
    }
}

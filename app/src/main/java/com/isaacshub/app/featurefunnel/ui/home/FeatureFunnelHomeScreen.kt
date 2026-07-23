package com.isaacshub.app.featurefunnel.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.featurefunnel.data.FeaturePromptEntity
import com.isaacshub.app.featurefunnel.data.PromptStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureFunnelHomeScreen(
    onNavigateToEdit: (Long?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: FeatureFunnelHomeViewModel = viewModel(
        factory = FeatureFunnelHomeViewModel.Factory(
            app.featureFunnelRepository,
            app.featureFunnelPreferencesRepository,
            context
        )
    )

    val prompts by viewModel.prompts.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feature Funnel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.checkNow() }) {
                        Icon(Icons.Default.Refresh, "Check Now")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEdit(null) }) {
                Icon(Icons.Default.Add, "Add Prompt")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enabled", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = preferences.enabled,
                            onCheckedChange = { viewModel.setEnabled(it) }
                        )
                    }
                }
            }

            // Prompt list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(prompts, key = { it.id }) { prompt ->
                    PromptCard(
                        prompt = prompt,
                        onEdit = { onNavigateToEdit(prompt.id) },
                        onDelete = { viewModel.deletePrompt(prompt) },
                        onRetry = { viewModel.retryPrompt(prompt.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PromptCard(
    prompt: FeaturePromptEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = prompt.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = prompt.promptText.take(100),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: ${prompt.status.name}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

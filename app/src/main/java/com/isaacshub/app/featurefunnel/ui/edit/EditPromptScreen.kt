package com.isaacshub.app.featurefunnel.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPromptScreen(
    promptId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: EditPromptViewModel = viewModel(
        factory = EditPromptViewModel.Factory(app.featureFunnelRepository, promptId)
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (promptId == null) "New Feature" else "Edit Feature") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save()
                            onSaved()
                        },
                        enabled = uiState.canSave
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.promptText,
                onValueChange = { viewModel.updatePromptText(it) },
                label = { Text("Prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 10
            )

            OutlinedTextField(
                value = uiState.priority.toString(),
                onValueChange = { viewModel.updatePriority(it.toIntOrNull() ?: 0) },
                label = { Text("Priority") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

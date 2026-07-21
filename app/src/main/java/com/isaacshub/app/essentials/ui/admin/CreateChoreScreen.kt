package com.isaacshub.app.essentials.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChoreScreen(
    choreId: Long?,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: CreateChoreViewModel = viewModel(
        factory = CreateChoreViewModel.Factory(app.essentialsRepository, choreId)
    )
    val state by viewModel.uiState.collectAsState()
    val children by viewModel.children.collectAsState()

    // Navigate back when saved
    LaunchedEffect(state.saved) {
        if (state.saved) onSave()
    }

    val isEditing = choreId != null
    val title = if (isEditing) "Edit Chore" else "Create Chore"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = state.name.isNotBlank() && state.selectedDays.isNotEmpty() && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show error if present
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Chore Name") },
                placeholder = { Text("e.g., Make bed, Do dishes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                placeholder = { Text("Detailed instructions for completing this chore") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !state.isLoading
            )

            HorizontalDivider()

            Text(
                text = "Photo Verification",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Require photo to complete")
                Switch(
                    checked = state.requirePhoto,
                    onCheckedChange = viewModel::setRequirePhoto,
                    enabled = !state.isLoading
                )
            }

            if (state.requirePhoto) {
                OutlinedTextField(
                    value = state.photoRequirement ?: "",
                    onValueChange = viewModel::setPhotoRequirement,
                    label = { Text("What should be in the photo?") },
                    placeholder = { Text("e.g., A neat bedroom with bed made and floor clear") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = !state.isLoading,
                    supportingText = {
                        Text("This description will be used by AI to verify the photo matches the requirement")
                    }
                )
            }

            HorizontalDivider()

            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Select which days this chore should be done:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Days of week chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DayOfWeek.MONDAY to "Monday",
                    DayOfWeek.TUESDAY to "Tuesday",
                    DayOfWeek.WEDNESDAY to "Wednesday",
                    DayOfWeek.THURSDAY to "Thursday",
                    DayOfWeek.FRIDAY to "Friday",
                    DayOfWeek.SATURDAY to "Saturday",
                    DayOfWeek.SUNDAY to "Sunday"
                ).forEach { (day, label) ->
                    FilterChip(
                        selected = state.selectedDays.contains(day),
                        onClick = { viewModel.toggleDay(day) },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )
                }
            }

            if (state.selectedDays.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "This chore will be assigned on: ${state.selectedDays.sortedBy { it.ordinal }.joinToString(", ") { day -> day.name.lowercase().replaceFirstChar { char -> char.uppercase() } }}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "Assigned To",
                style = MaterialTheme.typography.titleMedium
            )

            // Family member selection
            if (children.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No family members yet.\nCreate accounts in Manage Family first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { child ->
                        FilterChip(
                            selected = state.assignedChildIds.contains(child.id),
                            onClick = { viewModel.toggleChild(child.id) },
                            label = { Text(child.displayName) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                            leadingIcon = if (state.assignedChildIds.contains(child.id)) {
                                { Icon(Icons.Filled.Person, contentDescription = null) }
                            } else null
                        )
                    }
                }

                if (state.assignedChildIds.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "This chore will be assigned to: ${children.filter { state.assignedChildIds.contains(it.id) }.joinToString(", ") { it.displayName }}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

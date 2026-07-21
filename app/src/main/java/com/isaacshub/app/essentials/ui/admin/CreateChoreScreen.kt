package com.isaacshub.app.essentials.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChoreScreen(
    choreId: Long?,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var choreName by remember { mutableStateOf("") }
    var choreDescription by remember { mutableStateOf("") }
    var photoRequirement by remember { mutableStateOf("") }
    var requirePhoto by remember { mutableStateOf(false) }

    // Days of week selection
    var selectedDays by remember {
        mutableStateOf(setOf<DayOfWeek>())
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
                        onClick = {
                            // TODO: Save chore logic
                            onSave()
                        },
                        enabled = choreName.isNotBlank() && selectedDays.isNotEmpty()
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = choreName,
                onValueChange = { choreName = it },
                label = { Text("Chore Name") },
                placeholder = { Text("e.g., Make bed, Do dishes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = choreDescription,
                onValueChange = { choreDescription = it },
                label = { Text("Description") },
                placeholder = { Text("Detailed instructions for completing this chore") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
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
                    checked = requirePhoto,
                    onCheckedChange = { requirePhoto = it }
                )
            }

            if (requirePhoto) {
                OutlinedTextField(
                    value = photoRequirement,
                    onValueChange = { photoRequirement = it },
                    label = { Text("What should be in the photo?") },
                    placeholder = { Text("e.g., A neat bedroom with bed made and floor clear") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
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
                        selected = selectedDays.contains(day),
                        onClick = {
                            selectedDays = if (selectedDays.contains(day)) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (selectedDays.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "This chore will be assigned on: ${selectedDays.sortedBy { it.ordinal }.joinToString(", ") { day -> day.name.lowercase().replaceFirstChar { char -> char.uppercase() } }}",
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

            // TODO: Add family member selection
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
        }
    }
}

package com.isaacshub.app.activitymapper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.isaacshub.app.App
import com.isaacshub.app.activitymapper.data.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRichPresenceProfileScreen(
    profileId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()

    val vaultConnection by app.vaultPreferencesRepository.connection.collectAsState(initial = null)

    val repository = remember(vaultConnection) {
        vaultConnection?.let {
            ActivityMapperRepository(ActivityMapperApiClient(it))
        }
    }

    val profiles by repository?.richPresenceProfiles?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    // Load data
    LaunchedEffect(repository) {
        repository?.loadRichPresenceProfiles()
    }

    val existingProfile = profiles.find { it.id == profileId }

    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var state by remember { mutableStateOf(existingProfile?.state ?: "") }
    var details by remember { mutableStateOf(existingProfile?.details ?: "") }
    var largeImageKey by remember { mutableStateOf(existingProfile?.largeImageKey ?: "") }
    var largeImageText by remember { mutableStateOf(existingProfile?.largeImageText ?: "") }
    var smallImageKey by remember { mutableStateOf(existingProfile?.smallImageKey ?: "") }
    var smallImageText by remember { mutableStateOf(existingProfile?.smallImageText ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profileId == null) "New Rich Presence Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null

                                val profile = DiscordRichPresenceProfile(
                                    id = profileId ?: UUID.randomUUID().toString(),
                                    name = name,
                                    state = state.takeIf { it.isNotBlank() },
                                    details = details.takeIf { it.isNotBlank() },
                                    largeImageKey = largeImageKey.takeIf { it.isNotBlank() },
                                    largeImageText = largeImageText.takeIf { it.isNotBlank() },
                                    smallImageKey = smallImageKey.takeIf { it.isNotBlank() },
                                    smallImageText = smallImageText.takeIf { it.isNotBlank() }
                                )

                                val result = if (repository == null) {
                                    Result.failure(Exception("Not connected to server"))
                                } else if (profileId == null) {
                                    repository.createRichPresenceProfile(profile)
                                } else {
                                    repository.updateRichPresenceProfile(profile)
                                }

                                result.fold(
                                    onSuccess = {
                                        onSaved()
                                    },
                                    onFailure = { exception ->
                                        errorMessage = exception.message
                                        isSaving = false
                                    }
                                )
                            }
                        },
                        enabled = !isSaving && name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Error message
            errorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Info card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Variable Interpolation",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Use {{VARIABLE_NAME}} to insert variable values. Available variables:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        VariableType.values().forEach { variable ->
                            Text(
                                text = "  {{${variable.name}}}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // Profile Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Delivering Mail, Idle, Sleeping") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Details (Top text)
            item {
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details (Top Text)") },
                    placeholder = { Text("e.g., On Route") },
                    supportingText = { Text("Supports variable interpolation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // State (Bottom text)
            item {
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State (Bottom Text)") },
                    placeholder = { Text("e.g., Stop {{CURRENT_STOP_NUMBER}}/{{TOTAL_STOPS}}") },
                    supportingText = { Text("Supports variable interpolation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Large Image
            item {
                Text(
                    text = "Large Image",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                OutlinedTextField(
                    value = largeImageKey,
                    onValueChange = { largeImageKey = it },
                    label = { Text("Large Image Key") },
                    placeholder = { Text("e.g., mail_truck") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = largeImageText,
                    onValueChange = { largeImageText = it },
                    label = { Text("Large Image Hover Text") },
                    placeholder = { Text("e.g., Delivering packages") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Small Image
            item {
                Text(
                    text = "Small Image",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                OutlinedTextField(
                    value = smallImageKey,
                    onValueChange = { smallImageKey = it },
                    label = { Text("Small Image Key") },
                    placeholder = { Text("e.g., usps_logo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = smallImageText,
                    onValueChange = { smallImageText = it },
                    label = { Text("Small Image Hover Text") },
                    placeholder = { Text("e.g., USPS") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (isSaving) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

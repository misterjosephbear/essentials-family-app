package com.isaacshub.app.activitymapper.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.activitymapper.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityMapperHomeScreen(
    onEditRule: (Int?) -> Unit,
    onEditProfile: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App

    // Create repository and API client
    val vaultConnection by app.vaultPreferencesRepository.connection.collectAsState(initial = null)

    val repository = remember(vaultConnection) {
        vaultConnection?.let {
            ActivityMapperRepository(ActivityMapperApiClient(it))
        }
    }

    val viewModel: ActivityMapperViewModel? = repository?.let { repo ->
        viewModel(factory = ActivityMapperViewModel.Factory(repo))
    }

    val rules: List<AutomationRule> = viewModel?.rules?.collectAsState()?.value ?: emptyList()
    val profiles: List<DiscordRichPresenceProfile> = viewModel?.richPresenceProfiles?.collectAsState()?.value ?: emptyList()
    val variables: List<VariableValue> = viewModel?.variables?.collectAsState()?.value ?: emptyList()
    val isLoading: Boolean = viewModel?.isLoading?.collectAsState()?.value ?: false
    val error: String? = viewModel?.error?.collectAsState()?.value

    var showDeleteRuleDialog by remember { mutableStateOf<Int?>(null) }
    var showDeleteProfileDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Mapper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel?.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
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
            error?.let { errorMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel?.clearError() }) {
                                Icon(Icons.Default.Close, "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading && rules.isEmpty() && profiles.isEmpty() && variables.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Variables Section
            item {
                SectionHeader(
                    title = "Current Variables",
                    icon = Icons.Default.Dashboard
                )
            }

            if (variables.isEmpty() && !isLoading) {
                item {
                    Text(
                        text = "No variables available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(variables, key = { it.type.name }) { variable ->
                    VariableCard(variable)
                }
            }

            // Automation Rules Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Automation Rules",
                        icon = Icons.Default.Rule,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onEditRule(null) }) {
                        Icon(Icons.Default.Add, "Add Rule")
                    }
                }
            }

            if (rules.isEmpty() && !isLoading) {
                item {
                    Text(
                        text = "No automation rules configured",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onToggleEnabled = { viewModel?.toggleRuleEnabled(rule) },
                        onEdit = { onEditRule(rule.id) },
                        onDelete = { showDeleteRuleDialog = rule.id }
                    )
                }
            }

            // Rich Presence Profiles Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Discord Rich Presence Profiles",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onEditProfile(null) }) {
                        Icon(Icons.Default.Add, "Add Profile")
                    }
                }
            }

            if (profiles.isEmpty() && !isLoading) {
                item {
                    Text(
                        text = "No Rich Presence profiles configured",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(profiles, key = { it.id }) { profile ->
                    RichPresenceProfileCard(
                        profile = profile,
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { showDeleteProfileDialog = profile.id }
                    )
                }
            }
        }
    }

    // Delete Rule Confirmation Dialog
    showDeleteRuleDialog?.let { ruleId ->
        AlertDialog(
            onDismissRequest = { showDeleteRuleDialog = null },
            title = { Text("Delete Rule") },
            text = { Text("Are you sure you want to delete this automation rule?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.deleteRule(ruleId)
                        showDeleteRuleDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRuleDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Profile Confirmation Dialog
    showDeleteProfileDialog?.let { profileId ->
        AlertDialog(
            onDismissRequest = { showDeleteProfileDialog = null },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete this Rich Presence profile?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.deleteRichPresenceProfile(profileId)
                        showDeleteProfileDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProfileDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VariableCard(variable: VariableValue) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = variable.type.name.replace("_", " "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Value: ${variable.value}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Updated: ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(variable.lastUpdated))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: AutomationRule,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Priority: ${rule.priority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { onToggleEnabled() }
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Conditions (${rule.logicalOperator.name}):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            rule.conditions.forEach { condition ->
                Text(
                    text = "  ${condition.variable.name.replace("_", " ")} ${condition.operator.symbol} ${condition.value}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Actions:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            rule.actions.forEach { action ->
                when (action) {
                    is Action.SetDiscordRichPresence -> {
                        Text(
                            text = "  Set Discord Rich Presence: ${action.profileId}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RichPresenceProfileCard(
    profile: DiscordRichPresenceProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            profile.details?.let {
                Text(
                    text = "Details: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            profile.state?.let {
                Text(
                    text = "State: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (profile.largeImageKey != null || profile.smallImageKey != null) {
                Text(
                    text = "Images: ${listOfNotNull(profile.largeImageKey, profile.smallImageKey).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

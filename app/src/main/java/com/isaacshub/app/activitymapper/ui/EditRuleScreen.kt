package com.isaacshub.app.activitymapper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.isaacshub.app.App
import com.isaacshub.app.activitymapper.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleScreen(
    ruleId: Int?,
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

    val rules by repository?.rules?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val profiles by repository?.richPresenceProfiles?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    // Load data
    LaunchedEffect(repository) {
        repository?.loadRules()
        repository?.loadRichPresenceProfiles()
    }

    val existingRule = rules.find { it.id == ruleId }

    var name by remember { mutableStateOf(existingRule?.name ?: "") }
    var enabled by remember { mutableStateOf(existingRule?.enabled ?: true) }
    var priority by remember { mutableStateOf(existingRule?.priority?.toString() ?: "100") }
    var logicalOperator by remember { mutableStateOf(existingRule?.logicalOperator ?: LogicalOperator.AND) }
    var conditions: MutableList<Condition> by remember { mutableStateOf(existingRule?.conditions?.toMutableList() ?: mutableListOf()) }
    var actions: MutableList<com.isaacshub.app.activitymapper.data.Action> by remember { mutableStateOf(existingRule?.actions?.toMutableList() ?: mutableListOf()) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == null) "New Rule" else "Edit Rule") },
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

                                val rule = AutomationRule(
                                    id = ruleId ?: 0,
                                    name = name,
                                    enabled = enabled,
                                    priority = priority.toIntOrNull() ?: 100,
                                    conditions = conditions,
                                    logicalOperator = logicalOperator,
                                    actions = actions
                                )

                                val result = if (repository == null) {
                                    Result.failure(Exception("Not connected to server"))
                                } else if (ruleId == null) {
                                    repository.createRule(rule)
                                } else {
                                    repository.updateRule(rule)
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
                        enabled = !isSaving && name.isNotBlank() && conditions.isNotEmpty() && actions.isNotEmpty()
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

            // Basic Info
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Priority (higher = evaluated first)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Conditions
            item {
                Text(
                    text = "Conditions",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Combine conditions with:", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = logicalOperator == LogicalOperator.AND,
                            onClick = { logicalOperator = LogicalOperator.AND },
                            label = { Text("AND") }
                        )
                        FilterChip(
                            selected = logicalOperator == LogicalOperator.OR,
                            onClick = { logicalOperator = LogicalOperator.OR },
                            label = { Text("OR") }
                        )
                    }
                }
            }

            itemsIndexed(conditions) { index, condition ->
                ConditionEditor(
                    condition = condition,
                    onUpdate = {
                        conditions = conditions.toMutableList().apply { this[index] = it }
                    },
                    onDelete = {
                        conditions = conditions.toMutableList().apply { removeAt(index) }
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        conditions = conditions.toMutableList().apply {
                            add(
                                Condition(
                                    variable = VariableType.ROUTE_PLAY_MODE_ACTIVE,
                                    operator = ComparisonOperator.EQUALS,
                                    value = true
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Condition")
                }
            }

            // Actions
            item {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            itemsIndexed(actions) { index, action ->
                ActionEditor(
                    action = action,
                    profiles = profiles,
                    onUpdate = {
                        actions = actions.toMutableList().apply { this[index] = it }
                    },
                    onDelete = {
                        actions = actions.toMutableList().apply { removeAt(index) }
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        if (profiles.isNotEmpty()) {
                            actions = actions.toMutableList().apply {
                                add(com.isaacshub.app.activitymapper.data.Action.SetDiscordRichPresence(profiles.first().id))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = profiles.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Action")
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionEditor(
    condition: Condition,
    onUpdate: (Condition) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Condition", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete")
                }
            }

            // Variable selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = condition.variable.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Variable") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    VariableType.values().forEach { variable ->
                        DropdownMenuItem(
                            text = { Text(variable.name.replace("_", " ")) },
                            onClick = {
                                onUpdate(condition.copy(variable = variable))
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Operator selection
            ExposedDropdownMenuBox(
                expanded = operatorExpanded,
                onExpandedChange = { operatorExpanded = it }
            ) {
                OutlinedTextField(
                    value = condition.operator.symbol,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Operator") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(operatorExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = operatorExpanded,
                    onDismissRequest = { operatorExpanded = false }
                ) {
                    ComparisonOperator.values().forEach { operator ->
                        DropdownMenuItem(
                            text = { Text(operator.symbol) },
                            onClick = {
                                onUpdate(condition.copy(operator = operator))
                                operatorExpanded = false
                            }
                        )
                    }
                }
            }

            // Value input
            OutlinedTextField(
                value = condition.value.toString(),
                onValueChange = {
                    val newValue = when {
                        it.equals("true", ignoreCase = true) -> true
                        it.equals("false", ignoreCase = true) -> false
                        it.toIntOrNull() != null -> it.toInt()
                        it.toDoubleOrNull() != null -> it.toDouble()
                        else -> it
                    }
                    onUpdate(condition.copy(value = newValue))
                },
                label = { Text("Value") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionEditor(
    action: Action,
    profiles: List<DiscordRichPresenceProfile>,
    onUpdate: (Action) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set Discord Rich Presence", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete")
                }
            }

            when (action) {
                is Action.SetDiscordRichPresence -> {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = profiles.find { it.id == action.profileId }?.name ?: action.profileId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        onUpdate(Action.SetDiscordRichPresence(profile.id))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

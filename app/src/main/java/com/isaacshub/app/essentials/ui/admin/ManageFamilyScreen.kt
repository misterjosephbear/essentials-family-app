package com.isaacshub.app.essentials.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.essentials.data.ChildAccountEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFamilyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: ManageFamilyViewModel = viewModel(
        factory = ManageFamilyViewModel.Factory(app.essentialsRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val children by viewModel.children.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ChildAccountEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Family") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add child account")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Child Accounts",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Manage family member accounts for the Essentials app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error message
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

            // Children list
            if (children.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No child accounts yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { child ->
                        ChildAccountCard(
                            child = child,
                            onEdit = { editingChild = child },
                            onDelete = { viewModel.deleteChild(child.id) }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showAddDialog || editingChild != null) {
        ChildAccountDialog(
            child = editingChild,
            onDismiss = {
                showAddDialog = false
                editingChild = null
            },
            onSave = { username, displayName, password ->
                if (editingChild != null) {
                    viewModel.updateChild(editingChild!!.id, username, displayName, password)
                } else {
                    viewModel.createChild(username, displayName, password ?: "changeme")
                }
                showAddDialog = false
                editingChild = null
            }
        )
    }
}

@Composable
private fun ChildAccountCard(
    child: ChildAccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = child.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Username: ${child.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit account")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete account")
                }
            }
        }
    }
}

@Composable
private fun ChildAccountDialog(
    child: ChildAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (username: String, displayName: String, password: String?) -> Unit
) {
    var username by remember { mutableStateOf(child?.username ?: "") }
    var displayName by remember { mutableStateOf(child?.displayName ?: "") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (child != null) "Edit Child Account" else "Add Child Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g., Sarah, John") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("e.g., sarah123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Used to log into the Essentials app")
                    }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (child != null) "New Password (optional)" else "Password") },
                    placeholder = { Text(if (child != null) "Leave blank to keep current" else "Enter password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(if (child != null) "Only fill to change password" else "Password for Essentials app login")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        username,
                        displayName,
                        password.takeIf { it.isNotBlank() }
                    )
                },
                enabled = username.isNotBlank() && displayName.isNotBlank() &&
                         (child != null || password.isNotBlank())
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

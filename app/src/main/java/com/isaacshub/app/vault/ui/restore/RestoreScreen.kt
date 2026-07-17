package com.isaacshub.app.vault.ui.restore

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    viewModel: RestoreViewModel,
    onNavigateBack: () -> Unit
) {
    val databases by viewModel.databases.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore App Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "⚠️ Warning",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "Restoring will overwrite your current data for the selected databases. " +
                        "Only select the databases you want to restore. The app will restart after restoration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Backup Info Card
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Backup Information",
                        style = MaterialTheme.typography.titleMedium
                    )

                    lastBackupTime?.let { timestamp ->
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                        Text(
                            "Last backup: ${dateFormat.format(Date(timestamp))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } ?: run {
                        Text(
                            "No backup information available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Database Selection
            Text(
                "Select databases to restore:",
                style = MaterialTheme.typography.titleMedium
            )

            databases.forEach { database ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.toggleDatabaseSelection(database.name) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                database.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                database.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = database.selected,
                            onCheckedChange = { viewModel.toggleDatabaseSelection(database.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Restore Button
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = databases.any { it.selected } && restoreState !is RestoreState.Loading
            ) {
                if (restoreState is RestoreState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restoring...")
                } else {
                    Text("Restore Selected Databases")
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You are about to restore the following databases:")
                    databases.filter { it.selected }.forEach { db ->
                        Text("• ${db.displayName}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will overwrite your current data for these databases. The app will restart after restoration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.restoreSelectedDatabases()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog with Restart Prompt
    if (restoreState is RestoreState.Success) {
        val restoredDatabases = (restoreState as RestoreState.Success).restoredDatabases
        LaunchedEffect(Unit) {
            showRestartDialog = true
        }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Restore Complete") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Successfully restored:")
                        restoredDatabases.forEach { dbName ->
                            Text("• $dbName", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The app needs to restart to use the restored data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Restart the app
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        }
                    ) {
                        Text("Restart App")
                    }
                }
            )
        }
    }

    // Error Dialog
    if (restoreState is RestoreState.Error) {
        val errorMessage = (restoreState as RestoreState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Restore Failed") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("OK")
                }
            }
        )
    }
}

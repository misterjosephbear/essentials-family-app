package com.isaacshub.essentials.ui.choredetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.isaacshub.essentials.EssentialsApp
import com.isaacshub.essentials.data.local.entities.CompletionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreDetailScreen(
    choreId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: (Long) -> Unit,
    viewModel: ChoreDetailViewModel = viewModel(
        factory = ChoreDetailViewModel.Factory(
            choreId = choreId,
            essentialsRepository = (LocalContext.current.applicationContext as EssentialsApp).essentialsRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.chore?.name ?: "Chore Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.chore == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Chore not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chore info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.chore?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        if (!uiState.chore?.description.isNullOrBlank()) {
                            Text(
                                text = uiState.chore?.description ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.needsPhoto) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Photo required: ${uiState.chore?.photoRequirement}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Status card
                if (uiState.isCompleted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                uiState.isVerified -> MaterialTheme.colorScheme.primaryContainer
                                uiState.isPending -> MaterialTheme.colorScheme.tertiaryContainer
                                uiState.isRejected -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        uiState.isVerified -> Icons.Default.CheckCircle
                                        uiState.isPending -> Icons.Default.Info
                                        uiState.isRejected -> Icons.Default.Warning
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        uiState.isVerified -> MaterialTheme.colorScheme.onPrimaryContainer
                                        uiState.isPending -> MaterialTheme.colorScheme.onTertiaryContainer
                                        uiState.isRejected -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        uiState.isVerified -> "Verified ✓"
                                        uiState.isPending -> "Pending verification..."
                                        uiState.isRejected -> "Rejected - please try again"
                                        else -> "Completed"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (uiState.completion?.aiVerificationResult != null) {
                                Divider()
                                Text(
                                    text = "AI Feedback:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = uiState.completion?.aiVerificationResult ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Show photo if available
                            if (uiState.completion?.photoUri != null) {
                                Divider()
                                Text(
                                    text = "Photo:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                AsyncImage(
                                    model = uiState.completion?.photoUri,
                                    contentDescription = "Completion photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                )
                            }
                        }
                    }
                }

                // Error message
                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                if (uiState.canComplete || uiState.canRetake) {
                    if (uiState.needsPhoto) {
                        Button(
                            onClick = { onNavigateToCamera(choreId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.canRetake) "Retake Photo" else "Take Photo")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.completeWithoutPhoto(onNavigateBack) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark Complete")
                        }
                    }
                }
            }
        }
    }
}

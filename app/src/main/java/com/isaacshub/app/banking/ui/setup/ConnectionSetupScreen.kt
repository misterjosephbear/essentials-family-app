package com.isaacshub.app.banking.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.banking.data.BankingDatabase
import com.isaacshub.app.banking.data.BankingRepository
import com.isaacshub.app.banking.data.SimpleFINClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSetupScreen(
    onNavigateBack: () -> Unit,
    onConnectionAdded: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { BankingDatabase.getInstance(context) }
    val repository = remember { BankingRepository(database.bankingDao(), SimpleFINClient()) }
    val viewModel: ConnectionSetupViewModel = viewModel(
        factory = ConnectionSetupViewModel.Factory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate back on success
    LaunchedEffect(uiState) {
        if (uiState is ConnectionSetupUiState.Success) {
            onConnectionAdded()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState) {
        if (uiState is ConnectionSetupUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ConnectionSetupUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Bank Connection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "SimpleFIN Setup",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "SimpleFIN provides read-only access to your bank accounts for \$15/year. " +
                    "Visit https://beta-bridge.simplefin.org/claim to get a setup token.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var setupToken by remember { mutableStateOf("") }

            OutlinedTextField(
                value = setupToken,
                onValueChange = { setupToken = it },
                label = { Text("Setup Token") },
                placeholder = { Text("Paste your SimpleFIN setup token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = uiState !is ConnectionSetupUiState.Loading
            )

            Button(
                onClick = { viewModel.addSimpleFINConnection(setupToken) },
                modifier = Modifier.fillMaxWidth(),
                enabled = setupToken.isNotBlank() && uiState !is ConnectionSetupUiState.Loading
            ) {
                if (uiState is ConnectionSetupUiState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Text("Connect")
                }
            }

            if (uiState is ConnectionSetupUiState.Error) {
                Text(
                    (uiState as ConnectionSetupUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

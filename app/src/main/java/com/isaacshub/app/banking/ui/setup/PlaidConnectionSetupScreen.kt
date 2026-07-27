package com.isaacshub.app.banking.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.banking.data.BankingRepository
import com.isaacshub.app.banking.data.PlaidClient
import com.isaacshub.app.core.data.AppDatabase
import com.plaid.link.OpenPlaidLink
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaidConnectionSetupScreen(
    onNavigateBack: () -> Unit,
    onConnectionAdded: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val bankingDatabase = remember { com.isaacshub.app.banking.data.BankingDatabase.getInstance(context) }
    val repository = remember {
        BankingRepository(
            database.bankConnectionDao(),
            database.bankAccountDao(),
            bankingDatabase.budgetDao(),
            PlaidClient()
        )
    }
    val viewModel: PlaidConnectionSetupViewModel = viewModel(
        factory = PlaidConnectionSetupViewModel.Factory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Set up Plaid Link launcher
    val linkLauncher = rememberLauncherForActivityResult(OpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> viewModel.handlePlaidSuccess(result.publicToken)
            is LinkExit -> viewModel.handlePlaidExit()
        }
    }

    // Navigate back on success
    LaunchedEffect(uiState) {
        if (uiState is PlaidConnectionSetupUiState.Success) {
            onConnectionAdded()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState) {
        if (uiState is PlaidConnectionSetupUiState.Error) {
            snackbarHostState.showSnackbar((uiState as PlaidConnectionSetupUiState.Error).message)
        }
    }

    // Launch Plaid Link when we have a link token
    LaunchedEffect(uiState) {
        if (uiState is PlaidConnectionSetupUiState.ReadyToLaunchLink) {
            val linkToken = (uiState as PlaidConnectionSetupUiState.ReadyToLaunchLink).linkToken
            val config = LinkTokenConfiguration.Builder()
                .token(linkToken)
                .build()
            linkLauncher.launch(config)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PlaidConnectionSetupUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Connect Your Bank",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Securely connect your bank accounts using Plaid. Your credentials are never stored in this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = viewModel::startPlaidLink) {
                            Text("Connect Bank Account")
                        }
                    }
                }

                is PlaidConnectionSetupUiState.CreatingLinkToken,
                is PlaidConnectionSetupUiState.ExchangingToken -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            if (state is PlaidConnectionSetupUiState.CreatingLinkToken)
                                "Preparing connection..."
                            else
                                "Connecting your account...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is PlaidConnectionSetupUiState.ReadyToLaunchLink -> {
                    // Plaid Link will launch automatically via LaunchedEffect
                    CircularProgressIndicator()
                }

                is PlaidConnectionSetupUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }

                is PlaidConnectionSetupUiState.Success -> {
                    // Will navigate back via LaunchedEffect
                    CircularProgressIndicator()
                }
            }
        }
    }
}

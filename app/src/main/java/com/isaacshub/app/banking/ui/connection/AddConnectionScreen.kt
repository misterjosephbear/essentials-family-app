package com.isaacshub.app.banking.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.banking.data.BankingDatabase
import com.isaacshub.app.banking.data.BankingRepository
import com.isaacshub.app.banking.data.SimpleFINClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { BankingDatabase.getInstance(context) }
    val repository = remember { BankingRepository(database.bankingDao(), SimpleFINClient()) }
    val viewModel: AddConnectionViewModel = viewModel(
        factory = AddConnectionViewModel.Factory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var setupToken by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    // Navigate back on success
    LaunchedEffect(uiState) {
        if (uiState is AddConnectionUiState.Success) {
            onNavigateBack()
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
                "SimpleFIN Setup",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "SimpleFIN provides secure read-only access to your bank accounts for \$15/year. " +
                    "It supports thousands of US financial institutions including Acorns, credit unions, and investment platforms.",
                style = MaterialTheme.typography.bodyMedium
            )

            val annotatedText = buildAnnotatedString {
                append("1. Visit ")
                pushStringAnnotation(tag = "URL", annotation = "https://beta-bridge.simplefin.org/claim")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("SimpleFIN Claim")
                }
                pop()
                append(" to get a setup token\n")
                append("2. Paste the setup token below\n")
                append("3. Tap \"Connect\" to link your accounts")
            }

            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = setupToken,
                onValueChange = { setupToken = it },
                label = { Text("Setup Token") },
                placeholder = { Text("Paste your SimpleFIN setup token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is AddConnectionUiState.Processing
            )

            if (uiState is AddConnectionUiState.Error) {
                Text(
                    (uiState as AddConnectionUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.addSimpleFINConnection(setupToken) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AddConnectionUiState.Processing && setupToken.isNotBlank()
            ) {
                if (uiState is AddConnectionUiState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Connect")
                }
            }

            if (uiState is AddConnectionUiState.Processing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Connecting to SimpleFIN and fetching accounts...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

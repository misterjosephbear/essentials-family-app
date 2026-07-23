package com.isaacshub.essentials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.isaacshub.essentials.service.AppBlockingService
import com.isaacshub.essentials.ui.navigation.EssentialsNavGraph
import com.isaacshub.essentials.ui.navigation.Routes
import com.isaacshub.essentials.ui.theme.EssentialsTheme
import com.isaacshub.essentials.update.UpdateBanner
import com.isaacshub.essentials.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Determine start destination based on login state
        val app = application as EssentialsApp
        val isLoggedIn = app.authRepository.isLoggedIn()
        val startDestination = if (isLoggedIn) Routes.Home.route else Routes.Login.route

        // Start app blocking service if logged in
        if (isLoggedIn) {
            AppBlockingService.start(this)
        }

        setContent {
            EssentialsTheme {
                val updateViewModel: UpdateViewModel = viewModel()
                val updateState by updateViewModel.uiState.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        if (updateState.availableRelease != null || updateState.lastCheckFailure != null) {
                            UpdateBanner(
                                state = updateState,
                                onInstall = updateViewModel::installUpdate,
                                onDismiss = updateViewModel::dismiss
                            )
                        }
                        val navController = rememberNavController()
                        EssentialsNavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}

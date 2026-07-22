package com.isaacshub.essentials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.isaacshub.essentials.service.AppBlockingService
import com.isaacshub.essentials.ui.navigation.EssentialsNavGraph
import com.isaacshub.essentials.ui.navigation.Routes
import com.isaacshub.essentials.ui.theme.EssentialsTheme

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
                Surface(modifier = Modifier.fillMaxSize()) {
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

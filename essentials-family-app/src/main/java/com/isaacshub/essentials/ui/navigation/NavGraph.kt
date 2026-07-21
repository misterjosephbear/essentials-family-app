package com.isaacshub.essentials.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.isaacshub.essentials.ui.home.HomeScreen
import com.isaacshub.essentials.ui.login.LoginScreen

@Composable
fun EssentialsNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen()
        }

        composable(
            route = Routes.ChoreDetail.route,
            arguments = listOf(
                navArgument("choreId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val choreId = backStackEntry.arguments?.getLong("choreId") ?: return@composable
            // TODO: Implement ChoreDetailScreen in Step 5
        }

        composable(
            route = Routes.PhotoCapture.route,
            arguments = listOf(
                navArgument("choreId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val choreId = backStackEntry.arguments?.getLong("choreId") ?: return@composable
            // TODO: Implement PhotoCaptureScreen in Step 5
        }
    }
}

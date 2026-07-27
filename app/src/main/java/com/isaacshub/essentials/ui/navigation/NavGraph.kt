package com.isaacshub.essentials.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.isaacshub.essentials.ui.choredetail.ChoreDetailScreen
import com.isaacshub.essentials.ui.home.HomeScreen
import com.isaacshub.essentials.ui.login.LoginScreen
import com.isaacshub.essentials.ui.photo.PhotoCaptureScreen
import com.isaacshub.essentials.ui.setup.SetupScreen

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
                    navController.navigate(Routes.Setup.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                onChoreClick = { choreId ->
                    navController.navigate(Routes.ChoreDetail.createRoute(choreId))
                },
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.ChoreDetail.route,
            arguments = listOf(
                navArgument("choreId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val choreId = backStackEntry.arguments?.getLong("choreId") ?: return@composable
            ChoreDetailScreen(
                choreId = choreId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { id ->
                    navController.navigate(Routes.PhotoCapture.createRoute(id))
                }
            )
        }

        composable(
            route = Routes.PhotoCapture.route,
            arguments = listOf(
                navArgument("choreId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val choreId = backStackEntry.arguments?.getLong("choreId") ?: return@composable
            PhotoCaptureScreen(
                choreId = choreId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

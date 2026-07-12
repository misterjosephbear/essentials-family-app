package com.isaacshub.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.isaacshub.app.sleep.ui.edit.EditSessionScreen
import com.isaacshub.app.sleep.ui.history.HistoryScreen
import com.isaacshub.app.sleep.ui.home.HomeScreen
import com.isaacshub.app.sleep.ui.settings.SettingsScreen

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.HISTORY, "History", Icons.Filled.List),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun IsaacsHubApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onEditSession = { sessionId -> navController.navigate(Routes.editSession(sessionId)) },
                    onAddSession = { navController.navigate(Routes.editSession(null)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onEditSession = { sessionId -> navController.navigate(Routes.editSession(sessionId)) },
                    onAddSession = { navController.navigate(Routes.editSession(null)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.EDIT_SESSION_PATTERN) { backStackEntry ->
                val sessionId = Routes.parseSessionId(backStackEntry.arguments?.getString(Routes.EDIT_SESSION_ARG))
                EditSessionScreen(
                    sessionId = sessionId,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}

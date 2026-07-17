package com.isaacshub.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.isaacshub.app.landing.ui.LandingScreen
import com.isaacshub.app.routehelper.ui.builder.RouteBuilderScreen
import com.isaacshub.app.routehelper.ui.edit.RouteEditScreen
import com.isaacshub.app.routehelper.ui.home.RouteHelperHomeScreen
import com.isaacshub.app.routehelper.ui.player.RoutePlayerScreen
import com.isaacshub.app.routehelper.ui.test.RouteHelperTestScreen
import com.isaacshub.app.sleep.ui.edit.EditSessionScreen
import com.isaacshub.app.sleep.ui.history.HistoryScreen
import com.isaacshub.app.sleep.ui.home.HomeScreen
import com.isaacshub.app.sleep.ui.nap.NapScreen
import com.isaacshub.app.sleep.ui.settings.SettingsScreen
import com.isaacshub.app.timetracking.ui.editentry.EditTimeEntryScreen
import com.isaacshub.app.timetracking.ui.editroute.EditRouteScreen
import com.isaacshub.app.timetracking.ui.routes.RoutesScreen
import com.isaacshub.app.timetracking.ui.schedule.WeekScheduleScreen
import com.isaacshub.app.timetracking.ui.settings.TimeTrackerSettingsScreen
import com.isaacshub.app.timetracking.ui.week.TimeTrackingHomeScreen
import com.isaacshub.app.update.UpdateBanner
import com.isaacshub.app.update.UpdateViewModel
import com.isaacshub.app.vault.ui.home.VaultHomeScreen
import com.isaacshub.app.vault.ui.pairing.PairingScreen
import com.isaacshub.app.vault.ui.restore.RestoreScreen
import com.isaacshub.app.vault.ui.restore.RestoreViewModel

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val sleepDestinations = listOf(
    TopLevelDestination(Routes.SLEEP_HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.SLEEP_HISTORY, "History", Icons.Filled.List),
    TopLevelDestination(Routes.SLEEP_SETTINGS, "Settings", Icons.Filled.Settings)
)

private val timeTrackingDestinations = listOf(
    TopLevelDestination(Routes.TIME_HOME, "Week", Icons.Filled.Schedule),
    TopLevelDestination(Routes.TIME_ROUTES, "Routes", Icons.Filled.LocalShipping),
    TopLevelDestination(Routes.TIME_SETTINGS, "Settings", Icons.Filled.Settings)
)

private fun bottomDestinationsFor(route: String?): List<TopLevelDestination> = when (route) {
    Routes.SLEEP_HOME, Routes.SLEEP_HISTORY, Routes.SLEEP_SETTINGS -> sleepDestinations
    Routes.TIME_HOME, Routes.TIME_ROUTES, Routes.TIME_SETTINGS -> timeTrackingDestinations
    else -> emptyList()
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun IsaacsHubApp() {
    val navController = rememberNavController()
    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (updateState.availableRelease != null) {
            UpdateBanner(
                state = updateState,
                onInstall = updateViewModel::installUpdate,
                onDismiss = updateViewModel::dismiss
            )
        }
        IsaacsHubScaffold(navController, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IsaacsHubScaffold(navController: NavHostController, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val destinations = bottomDestinationsFor(currentDestination?.route)
            if (destinations.isNotEmpty()) {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LANDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LANDING) {
                LandingScreen(
                    onOpenSleep = { navController.navigate(Routes.SLEEP_HOME) },
                    onOpenTimeTracking = { navController.navigate(Routes.TIME_HOME) },
                    onOpenVault = { navController.navigate(Routes.VAULT_HOME) },
                    onOpenRouteHelper = { navController.navigate(Routes.ROUTE_HELPER_HOME) }
                )
            }

            composable(Routes.SLEEP_HOME) {
                HomeScreen(
                    onEditSession = { sessionId -> navController.navigate(Routes.editSession(sessionId)) },
                    onAddSession = { navController.navigate(Routes.editSession(null)) },
                    onOpenNap = { navController.navigate(Routes.SLEEP_NAP) }
                )
            }
            composable(Routes.SLEEP_NAP) {
                NapScreen()
            }
            composable(Routes.SLEEP_HISTORY) {
                HistoryScreen(
                    onEditSession = { sessionId -> navController.navigate(Routes.editSession(sessionId)) },
                    onAddSession = { navController.navigate(Routes.editSession(null)) }
                )
            }
            composable(Routes.SLEEP_SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.EDIT_SESSION_PATTERN) { backStackEntry ->
                val sessionId = Routes.parseId(backStackEntry.arguments?.getString(Routes.EDIT_SESSION_ARG))
                EditSessionScreen(
                    sessionId = sessionId,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.TIME_HOME) {
                TimeTrackingHomeScreen(
                    onEditEntry = { entryId -> navController.navigate(Routes.editTimeEntry(entryId)) },
                    onAddEntry = { navController.navigate(Routes.editTimeEntry(null)) },
                    onOpenSchedule = { navController.navigate(Routes.TIME_SCHEDULE) }
                )
            }
            composable(Routes.TIME_SCHEDULE) {
                WeekScheduleScreen(
                    onEditEntry = { entryId -> navController.navigate(Routes.editTimeEntry(entryId)) }
                )
            }
            composable(Routes.TIME_ROUTES) {
                RoutesScreen(
                    onEditRoute = { routeId -> navController.navigate(Routes.editRoute(routeId)) },
                    onAddRoute = { navController.navigate(Routes.editRoute(null)) }
                )
            }
            composable(Routes.TIME_SETTINGS) {
                TimeTrackerSettingsScreen()
            }
            composable(Routes.EDIT_TIME_ENTRY_PATTERN) { backStackEntry ->
                val entryId = Routes.parseId(backStackEntry.arguments?.getString(Routes.EDIT_TIME_ENTRY_ARG))
                EditTimeEntryScreen(
                    entryId = entryId,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.EDIT_ROUTE_PATTERN) { backStackEntry ->
                val routeId = Routes.parseId(backStackEntry.arguments?.getString(Routes.EDIT_ROUTE_ARG))
                EditRouteScreen(
                    routeId = routeId,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.VAULT_HOME) {
                VaultHomeScreen(
                    onPair = { navController.navigate(Routes.VAULT_PAIRING) },
                    onRestore = { navController.navigate(Routes.VAULT_RESTORE) }
                )
            }
            composable(Routes.VAULT_PAIRING) {
                PairingScreen(
                    onPaired = { navController.popBackStack() }
                )
            }
            composable(Routes.VAULT_RESTORE) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val app = context.applicationContext as com.isaacshub.app.App
                val restoreViewModel = RestoreViewModel(context, app.vaultPreferencesRepository)
                RestoreScreen(
                    viewModel = restoreViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ROUTE_HELPER_HOME) {
                RouteHelperHomeScreen(
                    onOpenRoute = { routeId -> navController.navigate(Routes.routeBuilder(routeId)) },
                    onOpenTestMode = { navController.navigate(Routes.ROUTE_HELPER_TEST) },
                    onEditRoute = { routeId -> navController.navigate(Routes.routeHelperEdit(routeId)) },
                    onPlayRoute = { routeId -> navController.navigate(Routes.routePlayer(routeId)) }
                )
            }
            composable(Routes.ROUTE_HELPER_TEST) {
                RouteHelperTestScreen()
            }
            composable(Routes.ROUTE_BUILDER_PATTERN) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString(Routes.ROUTE_BUILDER_ARG)?.toLongOrNull()
                if (routeId != null) {
                    RouteBuilderScreen(routeId = routeId)
                }
            }
            composable(Routes.ROUTE_HELPER_EDIT_PATTERN) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString(Routes.ROUTE_HELPER_EDIT_ARG)?.toLongOrNull()
                if (routeId != null) {
                    RouteEditScreen(routeId = routeId)
                }
            }
            composable(Routes.ROUTE_PLAYER_PATTERN) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString(Routes.ROUTE_PLAYER_ARG)?.toLongOrNull()
                if (routeId != null) {
                    RoutePlayerScreen(routeId = routeId, onDone = { navController.popBackStack() })
                }
            }
        }
    }
}

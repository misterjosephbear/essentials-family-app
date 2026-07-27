package com.isaacshub.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.isaacshub.app.banking.ui.home.BankingHomeScreen
import com.isaacshub.app.banking.ui.setup.PlaidConnectionSetupScreen
import com.isaacshub.app.landing.ui.LandingScreen
import com.isaacshub.app.routehelper.ui.builder.RouteBuilderScreen
import com.isaacshub.app.routehelper.ui.edit.RouteEditScreen
import com.isaacshub.app.routehelper.ui.home.RouteHelperHomeScreen
import com.isaacshub.app.routehelper.ui.player.RoutePlayerScreen
import com.isaacshub.app.routehelper.ui.test.RouteHelperTestScreen
import com.isaacshub.app.settings.ui.SettingsScreen as GeneralSettingsScreen
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
import com.isaacshub.app.essentials.ui.admin.EssentialsAdminHome
import com.isaacshub.app.essentials.ui.admin.CreateChoreScreen
import com.isaacshub.app.essentials.ui.admin.ManageFamilyScreen
import com.isaacshub.app.update.UpdateViewModel
import com.isaacshub.app.vault.ui.home.VaultHomeScreen
import com.isaacshub.app.vault.ui.pairing.PairingScreen
import com.isaacshub.app.vault.ui.restore.RestoreScreen
import com.isaacshub.app.vault.ui.restore.RestoreViewModel
import com.isaacshub.app.featurefunnel.ui.home.FeatureFunnelHomeScreen
import com.isaacshub.app.featurefunnel.ui.edit.EditPromptScreen
import com.isaacshub.app.activitymapper.ui.ActivityMapperHomeScreen
import com.isaacshub.app.activitymapper.ui.EditRuleScreen
import com.isaacshub.app.activitymapper.ui.EditRichPresenceProfileScreen

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
        if (updateState.availableRelease != null || updateState.lastCheckFailure != null) {
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
                    onOpenRouteHelper = { navController.navigate(Routes.ROUTE_HELPER_HOME) },
                    onOpenBanking = { navController.navigate(Routes.BANKING_HOME) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS_HOME) },
                    onOpenEssentials = { navController.navigate(Routes.ESSENTIALS_HOME) },
                    onOpenFeatureFunnel = { navController.navigate(Routes.FEATURE_FUNNEL_HOME) },
                    onOpenActivityMapper = { navController.navigate(Routes.ACTIVITY_MAPPER_HOME) }
                )
            }

            composable(Routes.SETTINGS_HOME) {
                GeneralSettingsScreen()
            }

            composable(Routes.BANKING_HOME) {
                BankingHomeScreen(
                    onAddConnection = { navController.navigate(Routes.BANKING_SETUP) },
                    onConfigureBudget = { navController.navigate(Routes.BANKING_BUDGET_CONFIG) }
                )
            }

            composable(Routes.BANKING_SETUP) {
                PlaidConnectionSetupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onConnectionAdded = { navController.popBackStack() }
                )
            }

            composable(Routes.BANKING_BUDGET_CONFIG) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val database = com.isaacshub.app.core.data.AppDatabase.getInstance(context)
                val bankingDatabase = com.isaacshub.app.banking.data.BankingDatabase.getInstance(context)
                val repository = com.isaacshub.app.banking.data.BankingRepository(
                    database.bankConnectionDao(),
                    database.bankAccountDao(),
                    bankingDatabase.budgetDao(),
                    com.isaacshub.app.banking.data.PlaidClient()
                )
                val viewModel: com.isaacshub.app.banking.ui.config.BudgetConfigViewModel = viewModel(
                    factory = com.isaacshub.app.banking.ui.config.BudgetConfigViewModel.Factory(repository)
                )
                com.isaacshub.app.banking.ui.config.BudgetConfigScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
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
                    onPlayRoute = { routeId -> navController.navigate(Routes.routePlayer(routeId)) },
                    onOpenAmazonScanner = { routeId -> navController.navigate(Routes.amazonScanner(routeId)) }
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
            composable(Routes.AMAZON_SCANNER_PATTERN) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString(Routes.AMAZON_SCANNER_ARG)?.toLongOrNull()
                if (routeId != null) {
                    com.isaacshub.app.routehelper.ui.scanner.AmazonRouteScannerScreen(
                        routeId = routeId,
                        onComplete = {
                            navController.navigate(Routes.routeHelperEdit(routeId)) {
                                popUpTo(Routes.ROUTE_HELPER_HOME)
                            }
                        }
                    )
                }
            }

            // Essentials routes
            composable(Routes.ESSENTIALS_HOME) {
                EssentialsAdminHome(
                    onBack = { navController.popBackStack() },
                    onCreateChore = { navController.navigate(Routes.ESSENTIALS_CREATE_CHORE) },
                    onEditChore = { choreId -> navController.navigate(Routes.essentialsEditChore(choreId)) },
                    onManageFamily = { navController.navigate(Routes.ESSENTIALS_MANAGE_FAMILY) }
                )
            }

            composable(Routes.ESSENTIALS_CREATE_CHORE) {
                CreateChoreScreen(
                    choreId = null,
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() }
                )
            }

            composable(Routes.ESSENTIALS_EDIT_CHORE_PATTERN) { backStackEntry ->
                val choreId = backStackEntry.arguments?.getString(Routes.ESSENTIALS_EDIT_CHORE_ARG)
                    ?.let { Routes.parseId(it) }
                CreateChoreScreen(
                    choreId = choreId,
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() }
                )
            }

            composable(Routes.ESSENTIALS_MANAGE_FAMILY) {
                ManageFamilyScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Feature Funnel routes
            composable(Routes.FEATURE_FUNNEL_HOME) {
                FeatureFunnelHomeScreen(
                    onNavigateToEdit = { promptId -> navController.navigate(Routes.featureFunnelEdit(promptId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.FEATURE_FUNNEL_EDIT_PATTERN) { backStackEntry ->
                val promptId = backStackEntry.arguments?.getString(Routes.FEATURE_FUNNEL_EDIT_ARG)
                    ?.let { Routes.parseId(it) }
                EditPromptScreen(
                    promptId = promptId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // Activity Mapper routes
            composable(Routes.ACTIVITY_MAPPER_HOME) {
                ActivityMapperHomeScreen(
                    onEditRule = { ruleId -> navController.navigate(Routes.activityMapperEditRule(ruleId)) },
                    onEditProfile = { profileId -> navController.navigate(Routes.activityMapperEditProfile(profileId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ACTIVITY_MAPPER_EDIT_RULE_PATTERN) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getString(Routes.ACTIVITY_MAPPER_EDIT_RULE_ARG)
                    ?.let { Routes.parseIntId(it) }
                EditRuleScreen(
                    ruleId = ruleId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Routes.ACTIVITY_MAPPER_EDIT_PROFILE_PATTERN) { backStackEntry ->
                val profileId = backStackEntry.arguments?.getString(Routes.ACTIVITY_MAPPER_EDIT_PROFILE_ARG)
                    ?.let { Routes.parseStringId(it) }
                EditRichPresenceProfileScreen(
                    profileId = profileId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}

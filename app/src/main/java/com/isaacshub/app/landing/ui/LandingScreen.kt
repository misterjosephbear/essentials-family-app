package com.isaacshub.app.landing.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.isaacshub.app.App
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class LandingCard(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onOpenSleep: () -> Unit,
    onOpenTimeTracking: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenRouteHelper: () -> Unit,
    onOpenBanking: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEssentials: () -> Unit = {},
    onOpenFeatureFunnel: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val prefs by app.preferencesRepository.userPreferences.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var showManageMenu by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }

    val allCards = remember {
        listOf(
            LandingCard(
                id = "sleep",
                icon = Icons.Filled.Bedtime,
                title = "Sleep Health",
                subtitle = "Track sleep sessions and sleep debt",
                onClick = onOpenSleep
            ),
            LandingCard(
                id = "time_tracking",
                icon = Icons.Filled.Schedule,
                title = "Time Tracking",
                subtitle = "Log route hours and evaluations, watch weekly overtime",
                onClick = onOpenTimeTracking
            ),
            LandingCard(
                id = "vault",
                icon = Icons.Filled.CloudUpload,
                title = "Photo Vault",
                subtitle = "Back up new photos to your own server",
                onClick = onOpenVault
            ),
            LandingCard(
                id = "route_helper",
                icon = Icons.Filled.Map,
                title = "Route Helper",
                subtitle = "Build a route live while driving it",
                onClick = onOpenRouteHelper
            ),
            LandingCard(
                id = "banking",
                icon = Icons.Filled.AccountBalance,
                title = "Banking",
                subtitle = "View all account balances in one place",
                onClick = onOpenBanking
            ),
            LandingCard(
                id = "essentials",
                icon = Icons.Filled.TaskAlt,
                title = "Essentials",
                subtitle = "Manage family chores and device restrictions",
                onClick = onOpenEssentials
            ),
            LandingCard(
                id = "feature_funnel",
                icon = Icons.Filled.AutoAwesome,
                title = "Feature Funnel",
                subtitle = "Queue feature requests for automated development",
                onClick = onOpenFeatureFunnel
            ),
            LandingCard(
                id = "settings",
                icon = Icons.Filled.Settings,
                title = "Settings",
                subtitle = "General app settings",
                onClick = onOpenSettings
            )
        )
    }

    // Apply custom order if set, then filter hidden cards
    val orderedAllCards = prefs?.landingCardOrder?.let { order ->
        val cardMap = allCards.associateBy { it.id }
        order.mapNotNull { id -> cardMap[id] }.plus(allCards.filter { it.id !in order })
    } ?: allCards

    val visibleCards = prefs?.let { preferences ->
        orderedAllCards.filter { it.id !in preferences.hiddenLandingCards }
    } ?: orderedAllCards

    var reorderableCards by remember { mutableStateOf(visibleCards) }

    // Update reorderable list when visible cards change
    if (!isReorderMode && reorderableCards != visibleCards) {
        reorderableCards = visibleCards
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderableCards = reorderableCards.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Isaac's Hub") },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (isReorderMode) {
                        IconButton(onClick = {
                            isReorderMode = false
                            scope.launch {
                                // Save the new order
                                val orderedIds = reorderableCards.map { it.id }
                                app.preferencesRepository.setLandingCardOrder(orderedIds)
                            }
                        }) {
                            Icon(Icons.Filled.Check, "Done reordering")
                        }
                    } else {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Reorder cards")
                        }
                        IconButton(onClick = { showManageMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "Manage cards")
                        }
                    }
                    DropdownMenu(
                        expanded = showManageMenu,
                        onDismissRequest = { showManageMenu = false }
                    ) {
                        allCards.forEach { card ->
                            val isHidden = prefs?.hiddenLandingCards?.contains(card.id) == true
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(card.title)
                                    }
                                },
                                onClick = {
                                    scope.launch {
                                        app.preferencesRepository.setCardVisibility(card.id, !isHidden)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (reorderableCards.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "All cards are hidden. Tap the menu to show cards.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(reorderableCards, key = { _, card -> card.id }) { index, card ->
                    ReorderableItem(reorderableLazyListState, key = card.id) { isDragging ->
                        ToolCard(
                            icon = card.icon,
                            title = card.title,
                            subtitle = card.subtitle,
                            onClick = if (isReorderMode) ({}) else card.onClick,
                            onLongClick = {
                                scope.launch {
                                    app.preferencesRepository.setCardVisibility(card.id, true)
                                }
                            },
                            showDragHandle = isReorderMode,
                            isDragging = isDragging
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showDragHandle: Boolean = false,
    isDragging: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (showDragHandle) ({}) else onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showDragHandle) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

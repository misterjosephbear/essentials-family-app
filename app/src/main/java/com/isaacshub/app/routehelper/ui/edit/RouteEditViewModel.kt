package com.isaacshub.app.routehelper.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.routehelper.data.RouteHelperRouteEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RouteEditUiState(
    val route: RouteHelperRouteEntity? = null,
    val stops: List<RoutedStopEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class RouteEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<App>().routeHelperRepository
    private val routeIdFlow = MutableStateFlow<Long?>(null)
    private val routeFlow = MutableStateFlow<RouteHelperRouteEntity?>(null)

    fun start(routeId: Long) {
        if (routeIdFlow.value == routeId) return
        routeIdFlow.value = routeId
        viewModelScope.launch { routeFlow.value = repository.getRoute(routeId) }
    }

    private val stopsFlow = routeIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeStops(id)
    }

    val uiState: StateFlow<RouteEditUiState> = kotlinx.coroutines.flow.combine(routeFlow, stopsFlow) { route, stops ->
        RouteEditUiState(route = route, stops = stops)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RouteEditUiState())

    fun moveUp(stop: RoutedStopEntity) {
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch { repository.moveStopUp(routeId, stop) }
    }

    fun moveDown(stop: RoutedStopEntity) {
        val routeId = routeIdFlow.value ?: return
        viewModelScope.launch { repository.moveStopDown(routeId, stop) }
    }

    fun deleteStop(stop: RoutedStopEntity) {
        viewModelScope.launch { repository.deleteStop(stop) }
    }
}

package com.isaacshub.app.routehelper.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.routehelper.data.CreateRouteResult
import com.isaacshub.app.routehelper.data.RouteHelperRouteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewRouteUiState(
    val creating: Boolean = false,
    val error: String? = null,
    val createdRouteId: Long? = null
)

class RouteHelperHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<App>().routeHelperRepository

    val routes: StateFlow<List<RouteHelperRouteEntity>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newRouteState = MutableStateFlow(NewRouteUiState())
    val newRouteState: StateFlow<NewRouteUiState> = _newRouteState

    fun createRoute(name: String, zipCode: String) {
        if (name.isBlank() || zipCode.isBlank()) {
            _newRouteState.value = NewRouteUiState(error = "Enter a name and ZIP code")
            return
        }
        _newRouteState.value = NewRouteUiState(creating = true)
        viewModelScope.launch {
            when (val result = repository.createRoute(name.trim(), zipCode.trim())) {
                is CreateRouteResult.Success ->
                    _newRouteState.value = NewRouteUiState(createdRouteId = result.routeId)
                is CreateRouteResult.Failure ->
                    _newRouteState.value = NewRouteUiState(error = "Couldn't fetch addresses: ${result.reason}")
            }
        }
    }

    fun createAmazonRoute(zipCode: String) {
        if (zipCode.isBlank()) {
            _newRouteState.value = NewRouteUiState(error = "Enter a ZIP code")
            return
        }
        _newRouteState.value = NewRouteUiState(creating = true)
        viewModelScope.launch {
            when (val result = repository.createAmazonRoute(zipCode.trim())) {
                is CreateRouteResult.Success ->
                    _newRouteState.value = NewRouteUiState(createdRouteId = result.routeId)
                is CreateRouteResult.Failure ->
                    _newRouteState.value = NewRouteUiState(error = "Couldn't fetch addresses: ${result.reason}")
            }
        }
    }

    fun consumeCreatedRoute() {
        _newRouteState.value = NewRouteUiState()
    }

    fun deleteRoute(route: RouteHelperRouteEntity) {
        viewModelScope.launch { repository.deleteRoute(route) }
    }
}

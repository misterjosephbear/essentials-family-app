package com.isaacshub.app.timetracking.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.timetracking.data.RouteEntity
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutesViewModel(private val repository: TimeTrackingRepository) : ViewModel() {

    val routes: StateFlow<List<RouteEntity>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(route: RouteEntity) {
        viewModelScope.launch { repository.deleteRoute(route) }
    }

    class Factory(private val repository: TimeTrackingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RoutesViewModel(repository) as T
    }
}

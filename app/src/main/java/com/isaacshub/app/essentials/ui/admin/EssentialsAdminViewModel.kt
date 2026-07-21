package com.isaacshub.app.essentials.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.essentials.data.ChildAccountEntity
import com.isaacshub.app.essentials.data.ChoreEntity
import com.isaacshub.app.essentials.data.EssentialsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EssentialsAdminViewModel(
    private val essentialsRepository: EssentialsRepository
) : ViewModel() {

    val chores: StateFlow<List<ChoreEntity>> = essentialsRepository.observeAllChores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val children: StateFlow<List<ChildAccountEntity>> = essentialsRepository.observeAllChildren()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteChore(choreId: Long) {
        viewModelScope.launch {
            essentialsRepository.deleteChore(choreId)
        }
    }

    class Factory(
        private val essentialsRepository: EssentialsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EssentialsAdminViewModel(essentialsRepository) as T
    }
}

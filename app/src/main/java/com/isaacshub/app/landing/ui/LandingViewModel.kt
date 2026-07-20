package com.isaacshub.app.landing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.landing.data.LandingPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ToolCardData(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

data class LandingUiState(
    val cards: List<ToolCardData>,
    val isReorderMode: Boolean = false
)

class LandingViewModel(
    private val repository: LandingPreferencesRepository,
    private val defaultCards: List<ToolCardData>
) : ViewModel() {

    private val _isReorderMode = MutableStateFlow(false)

    val uiState: StateFlow<LandingUiState> = combine(
        repository.cardOrder,
        _isReorderMode
    ) { savedOrder, isReorderMode ->
        val orderedCards = if (savedOrder != null) {
            // Reorder cards based on saved order
            val cardMap = defaultCards.associateBy { it.id }
            savedOrder.mapNotNull { id -> cardMap[id] }
                .plus(defaultCards.filter { it.id !in savedOrder }) // Add any new cards at the end
        } else {
            defaultCards
        }

        LandingUiState(
            cards = orderedCards,
            isReorderMode = isReorderMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LandingUiState(cards = defaultCards)
    )

    fun toggleReorderMode() {
        _isReorderMode.value = !_isReorderMode.value
    }

    fun moveCard(from: Int, to: Int) {
        val currentCards = uiState.value.cards.toMutableList()
        val card = currentCards.removeAt(from)
        currentCards.add(to, card)

        viewModelScope.launch {
            repository.saveCardOrder(currentCards.map { it.id })
        }
    }

    fun resetOrder() {
        viewModelScope.launch {
            repository.resetCardOrder()
        }
    }

    class Factory(
        private val repository: LandingPreferencesRepository,
        private val defaultCards: List<ToolCardData>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LandingViewModel(repository, defaultCards) as T
    }
}

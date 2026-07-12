package com.isaacshub.app.sleep.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.sleep.data.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class EditSessionUiState(
    val sessionId: Long? = null,
    val start: Instant = Instant.now().minusSeconds(8 * 3600L),
    val end: Instant = Instant.now(),
    val error: String? = null,
    val saved: Boolean = false
)

class EditSessionViewModel(
    private val sleepRepository: SleepRepository,
    sessionId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSessionUiState(sessionId = sessionId))
    val uiState: StateFlow<EditSessionUiState> = _uiState.asStateFlow()

    init {
        if (sessionId != null) {
            viewModelScope.launch {
                val existing = sleepRepository.getById(sessionId)
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        start = Instant.ofEpochMilli(existing.startEpochMillis),
                        end = Instant.ofEpochMilli(existing.endEpochMillis)
                    )
                }
            }
        }
    }

    fun setStart(instant: Instant) {
        _uiState.value = _uiState.value.copy(start = instant, error = null)
    }

    fun setEnd(instant: Instant) {
        _uiState.value = _uiState.value.copy(end = instant, error = null)
    }

    fun save() {
        val state = _uiState.value
        if (!state.end.isAfter(state.start)) {
            _uiState.value = state.copy(error = "End time must be after start time")
            return
        }
        viewModelScope.launch {
            val id = state.sessionId
            if (id == null) {
                sleepRepository.addManualSession(state.start, state.end)
            } else {
                sleepRepository.confirmSession(id, state.start, state.end)
            }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    class Factory(
        private val sleepRepository: SleepRepository,
        private val sessionId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditSessionViewModel(sleepRepository, sessionId) as T
    }
}

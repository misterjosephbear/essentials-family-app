package com.isaacshub.app.sleep.ui.nap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.sleep.nap.NapAlarmController
import com.isaacshub.app.sleep.nap.NapPhase
import com.isaacshub.app.sleep.nap.NapStateStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NapUiState(
    val phase: NapPhase = NapPhase.IDLE,
    val startEpochMillis: Long = 0L,
    val alarmEpochMillis: Long = 0L
)

/**
 * Polls [NapStateStore] rather than observing it, since the store is updated from a
 * BroadcastReceiver/Service that may run in a different lifecycle than this ViewModel - a short
 * poll interval is simpler than wiring a SharedPreferences listener into a Flow and is cheap
 * enough for the few minutes a nap lasts.
 */
class NapViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<NapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                refresh()
            }
        }
    }

    private fun loadState(): NapUiState {
        val stored = NapStateStore.read(getApplication())
        return NapUiState(
            phase = stored?.phase ?: NapPhase.IDLE,
            startEpochMillis = stored?.startEpochMillis ?: 0L,
            alarmEpochMillis = stored?.alarmEpochMillis ?: 0L
        )
    }

    fun refresh() {
        _uiState.value = loadState()
    }

    fun startNap(durationMinutes: Int) {
        NapAlarmController.startNap(getApplication(), durationMinutes)
        refresh()
    }

    fun cancelNap() {
        NapAlarmController.cancelNap(getApplication())
        refresh()
    }

    fun stopAlarm() {
        viewModelScope.launch {
            NapAlarmController.stopAlarmAndLog(getApplication())
            refresh()
        }
    }
}

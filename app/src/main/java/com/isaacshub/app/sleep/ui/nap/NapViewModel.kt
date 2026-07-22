package com.isaacshub.app.sleep.ui.nap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.App
import com.isaacshub.app.discord.DiscordRichPresenceManager
import com.isaacshub.app.sleep.nap.NapAlarmController
import com.isaacshub.app.sleep.nap.NapPhase
import com.isaacshub.app.sleep.nap.NapStateStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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

    private val prefsRepo = getApplication<App>().preferencesRepository
    private val sleepRepo = getApplication<App>().sleepRepository
    private val discordRpc = DiscordRichPresenceManager.getInstance(getApplication())

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

    fun startSleep() {
        viewModelScope.launch {
            try {
                val sleepStart = Instant.now()
                val tomorrow = LocalDate.now().plusDays(1)
                val tomorrowDayOfWeek = tomorrow.dayOfWeek

                val prefs = prefsRepo.userPreferences.first()
                val wakeMinutes = prefs.wakeTimeMinutesByDay[tomorrowDayOfWeek] ?: 420 // Default 7am

                val wakeTime = tomorrow.atTime(LocalTime.ofSecondOfDay((wakeMinutes * 60).toLong()))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

                // Add manual sleep session to repository
                sleepRepo.addManualSession(sleepStart, wakeTime)

                // Update Discord presence
                discordRpc.setSleepingPresence(sleepStart, wakeTime)

                refresh()
            } catch (e: Exception) {
                android.util.Log.e("NapViewModel", "Failed to start sleep", e)
            }
        }
    }
}

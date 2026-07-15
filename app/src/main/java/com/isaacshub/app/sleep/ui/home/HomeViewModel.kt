package com.isaacshub.app.sleep.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import com.isaacshub.app.sleep.data.SleepRepository
import com.isaacshub.app.sleep.data.SleepSessionEntity
import com.isaacshub.app.sleep.domain.computeDebt
import com.isaacshub.sleep.core.EnergyForecast
import com.isaacshub.sleep.core.EnergyForecastCalculator
import com.isaacshub.sleep.core.WindDownCalculator
import com.isaacshub.sleep.core.WindDownRecommendation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class HomeUiState(
    val loading: Boolean = true,
    val debtMinutes: Int = 0,
    val lastNight: SleepSessionEntity? = null,
    val pendingConfirmation: SleepSessionEntity? = null,
    val sleepNeedMinutes: Int = 480,
    val energyForecast: EnergyForecast? = null,
    val currentHoursAwake: Double? = null,
    val windDown: WindDownRecommendation? = null
)

class HomeViewModel(
    sleepRepository: SleepRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        sleepRepository.observeSince(Instant.now().minus(60, ChronoUnit.DAYS)),
        preferencesRepository.userPreferences
    ) { sessions, prefs ->
        val result = computeDebt(sessions, prefs)
        val lastNight = sessions.filter { it.confirmed }.maxByOrNull { it.endEpochMillis }
        val pending = sessions.filter { !it.confirmed }.maxByOrNull { it.endEpochMillis }

        val zone = ZoneId.systemDefault()
        val wakeZoned = lastNight?.let { Instant.ofEpochMilli(it.endEpochMillis).atZone(zone) }
        val energyForecast = wakeZoned?.let {
            val wakeHourOfDay = it.hour + it.minute / 60.0
            EnergyForecastCalculator.calculate(wakeHourOfDay, result.totalDebtMinutes)
        }
        val currentHoursAwake = wakeZoned?.let {
            Duration.between(it, ZonedDateTime.now(zone)).toMinutes() / 60.0
        }

        val tomorrow = LocalDate.now(zone).plusDays(1).dayOfWeek
        val tomorrowWakeMinutes = prefs.wakeTimeMinutesByDay.getValue(tomorrow)
        val windDown = WindDownCalculator.calculate(
            usualWakeHourOfDay = tomorrowWakeMinutes / 60.0,
            neededMinutesPerNight = prefs.sleepNeedMinutes,
            currentDebtMinutes = result.totalDebtMinutes
        )

        HomeUiState(
            loading = false,
            debtMinutes = result.totalDebtMinutes,
            lastNight = lastNight,
            pendingConfirmation = pending,
            sleepNeedMinutes = prefs.sleepNeedMinutes,
            energyForecast = energyForecast,
            currentHoursAwake = currentHoursAwake,
            windDown = windDown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    class Factory(
        private val sleepRepository: SleepRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(sleepRepository, preferencesRepository) as T
    }
}

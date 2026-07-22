package com.isaacshub.app.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore by preferencesDataStore(name = "isaacs_hub_prefs")

data class UserPreferences(
    val sleepNeedMinutes: Int = DEFAULT_SLEEP_NEED_MINUTES,
    val autoDetectEnabled: Boolean = false,
    val nightWindowStartHour: Int = DEFAULT_NIGHT_WINDOW_START_HOUR,
    val nightWindowEndHour: Int = DEFAULT_NIGHT_WINDOW_END_HOUR,
    val debtWindowDays: Int = DEFAULT_DEBT_WINDOW_DAYS,
    val hourlyRate: Double = DEFAULT_HOURLY_RATE,
    val overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER,
    val emaRate: Double = DEFAULT_EMA_RATE,
    val wakeTimeMinutesByDay: Map<DayOfWeek, Int> = DayOfWeek.entries.associateWith { DEFAULT_WAKE_TIME_MINUTES },
    val hiddenLandingCards: Set<String> = emptySet(),
    val landingCardOrder: List<String>? = null,
    val sleepDebtAdjustmentMinutes: Int = 0,
    val carryoverHoursAdjustment: Double = 0.0
) {
    companion object {
        const val DEFAULT_SLEEP_NEED_MINUTES = 480
        const val DEFAULT_NIGHT_WINDOW_START_HOUR = 20
        const val DEFAULT_NIGHT_WINDOW_END_HOUR = 12
        const val DEFAULT_DEBT_WINDOW_DAYS = 14
        const val DEFAULT_HOURLY_RATE = 0.0
        const val DEFAULT_OVERTIME_MULTIPLIER = 1.5
        const val DEFAULT_EMA_RATE = 0.0
        /** 7:00 AM, expressed as minutes since midnight. */
        const val DEFAULT_WAKE_TIME_MINUTES = 420
    }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val SLEEP_NEED_MINUTES = intPreferencesKey("sleep_need_minutes")
        val AUTO_DETECT_ENABLED = booleanPreferencesKey("auto_detect_enabled")
        val NIGHT_WINDOW_START_HOUR = intPreferencesKey("night_window_start_hour")
        val NIGHT_WINDOW_END_HOUR = intPreferencesKey("night_window_end_hour")
        val DEBT_WINDOW_DAYS = intPreferencesKey("debt_window_days")
        val HOURLY_RATE = doublePreferencesKey("hourly_rate")
        val OVERTIME_MULTIPLIER = doublePreferencesKey("overtime_multiplier")
        val EMA_RATE = doublePreferencesKey("ema_rate")
        val WAKE_TIME_MINUTES_BY_DAY: Map<DayOfWeek, Preferences.Key<Int>> = DayOfWeek.entries.associateWith { day ->
            intPreferencesKey("wake_time_${day.name.lowercase()}_minutes")
        }
        val HIDDEN_LANDING_CARDS = stringSetPreferencesKey("hidden_landing_cards")
        val LANDING_CARD_ORDER = stringPreferencesKey("landing_card_order")
        val SLEEP_DEBT_ADJUSTMENT_MINUTES = intPreferencesKey("sleep_debt_adjustment_minutes")
        val CARRYOVER_HOURS_ADJUSTMENT = doublePreferencesKey("carryover_hours_adjustment")
    }

    /** Every preference as-is, key name to value - used for backups so newly added prefs are included automatically. */
    val rawPreferences: Flow<Preferences> = context.dataStore.data

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            sleepNeedMinutes = prefs[Keys.SLEEP_NEED_MINUTES] ?: UserPreferences.DEFAULT_SLEEP_NEED_MINUTES,
            autoDetectEnabled = prefs[Keys.AUTO_DETECT_ENABLED] ?: false,
            nightWindowStartHour = prefs[Keys.NIGHT_WINDOW_START_HOUR]
                ?: UserPreferences.DEFAULT_NIGHT_WINDOW_START_HOUR,
            nightWindowEndHour = prefs[Keys.NIGHT_WINDOW_END_HOUR]
                ?: UserPreferences.DEFAULT_NIGHT_WINDOW_END_HOUR,
            debtWindowDays = prefs[Keys.DEBT_WINDOW_DAYS] ?: UserPreferences.DEFAULT_DEBT_WINDOW_DAYS,
            hourlyRate = prefs[Keys.HOURLY_RATE] ?: UserPreferences.DEFAULT_HOURLY_RATE,
            overtimeMultiplier = prefs[Keys.OVERTIME_MULTIPLIER] ?: UserPreferences.DEFAULT_OVERTIME_MULTIPLIER,
            emaRate = prefs[Keys.EMA_RATE] ?: UserPreferences.DEFAULT_EMA_RATE,
            wakeTimeMinutesByDay = DayOfWeek.entries.associateWith { day ->
                prefs[Keys.WAKE_TIME_MINUTES_BY_DAY.getValue(day)] ?: UserPreferences.DEFAULT_WAKE_TIME_MINUTES
            },
            hiddenLandingCards = prefs[Keys.HIDDEN_LANDING_CARDS] ?: emptySet(),
            landingCardOrder = prefs[Keys.LANDING_CARD_ORDER]?.split(",")?.filter { it.isNotBlank() },
            sleepDebtAdjustmentMinutes = prefs[Keys.SLEEP_DEBT_ADJUSTMENT_MINUTES] ?: 0,
            carryoverHoursAdjustment = prefs[Keys.CARRYOVER_HOURS_ADJUSTMENT] ?: 0.0
        )
    }

    suspend fun setSleepNeedMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SLEEP_NEED_MINUTES] = minutes }
    }

    suspend fun setAutoDetectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DETECT_ENABLED] = enabled }
    }

    suspend fun setNightWindow(startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.NIGHT_WINDOW_START_HOUR] = startHour
            it[Keys.NIGHT_WINDOW_END_HOUR] = endHour
        }
    }

    suspend fun setTimeTrackingRates(hourlyRate: Double, overtimeMultiplier: Double, emaRate: Double) {
        context.dataStore.edit {
            it[Keys.HOURLY_RATE] = hourlyRate
            it[Keys.OVERTIME_MULTIPLIER] = overtimeMultiplier
            it[Keys.EMA_RATE] = emaRate
        }
    }

    suspend fun setWakeTime(day: DayOfWeek, minutesSinceMidnight: Int) {
        context.dataStore.edit { it[Keys.WAKE_TIME_MINUTES_BY_DAY.getValue(day)] = minutesSinceMidnight }
    }

    suspend fun setCardVisibility(cardId: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val currentHidden = prefs[Keys.HIDDEN_LANDING_CARDS] ?: emptySet()
            prefs[Keys.HIDDEN_LANDING_CARDS] = if (hidden) {
                currentHidden + cardId
            } else {
                currentHidden - cardId
            }
        }
    }

    suspend fun setLandingCardOrder(cardIds: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANDING_CARD_ORDER] = cardIds.joinToString(",")
        }
    }

    suspend fun resetLandingCardOrder() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.LANDING_CARD_ORDER)
        }
    }

    suspend fun setSleepDebtAdjustment(minutes: Int) {
        context.dataStore.edit { it[Keys.SLEEP_DEBT_ADJUSTMENT_MINUTES] = minutes }
    }

    suspend fun setCarryoverHoursAdjustment(hours: Double) {
        context.dataStore.edit { it[Keys.CARRYOVER_HOURS_ADJUSTMENT] = hours }
    }
}

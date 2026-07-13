package com.isaacshub.app.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "isaacs_hub_prefs")

data class UserPreferences(
    val sleepNeedMinutes: Int = DEFAULT_SLEEP_NEED_MINUTES,
    val autoDetectEnabled: Boolean = false,
    val nightWindowStartHour: Int = DEFAULT_NIGHT_WINDOW_START_HOUR,
    val nightWindowEndHour: Int = DEFAULT_NIGHT_WINDOW_END_HOUR,
    val debtWindowDays: Int = DEFAULT_DEBT_WINDOW_DAYS,
    val hourlyRate: Double = DEFAULT_HOURLY_RATE,
    val overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER,
    val emaRate: Double = DEFAULT_EMA_RATE
) {
    companion object {
        const val DEFAULT_SLEEP_NEED_MINUTES = 480
        const val DEFAULT_NIGHT_WINDOW_START_HOUR = 20
        const val DEFAULT_NIGHT_WINDOW_END_HOUR = 12
        const val DEFAULT_DEBT_WINDOW_DAYS = 14
        const val DEFAULT_HOURLY_RATE = 0.0
        const val DEFAULT_OVERTIME_MULTIPLIER = 1.5
        const val DEFAULT_EMA_RATE = 0.0
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
            emaRate = prefs[Keys.EMA_RATE] ?: UserPreferences.DEFAULT_EMA_RATE
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
}

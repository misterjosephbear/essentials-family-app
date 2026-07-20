package com.isaacshub.app.landing.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.landingDataStore: DataStore<Preferences> by preferencesDataStore(name = "landing_preferences")

class LandingPreferencesRepository(context: Context) {
    private val dataStore = context.landingDataStore

    private object PreferencesKeys {
        val CARD_ORDER = stringPreferencesKey("card_order")
    }

    /**
     * Get the saved card order as a flow of card IDs.
     * Returns null if no custom order is set (use default order).
     */
    val cardOrder: Flow<List<String>?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CARD_ORDER]?.split(",")?.filter { it.isNotBlank() }
    }

    /**
     * Save a custom card order.
     */
    suspend fun saveCardOrder(cardIds: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CARD_ORDER] = cardIds.joinToString(",")
        }
    }

    /**
     * Reset to default card order.
     */
    suspend fun resetCardOrder() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CARD_ORDER)
        }
    }
}

package com.isaacshub.app.featurefunnel.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.featureFunnelDataStore by preferencesDataStore(name = "feature_funnel_prefs")

data class FeatureFunnelPreferences(
    val enabled: Boolean = false,                    // Master on/off switch
    val lastCheckEpochMillis: Long = 0L,            // Last time worker ran
    val lastCommitHash: String? = null,             // Last git commit we saw
    val discordChannelId: String? = null,           // Channel ID for Claude Code
    val checkIntervalMinutes: Int = 30              // How often to check
)

class FeatureFunnelPreferencesRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("feature_funnel_enabled")
        val LAST_CHECK_EPOCH_MILLIS = longPreferencesKey("last_check_epoch_millis")
        val LAST_COMMIT_HASH = stringPreferencesKey("last_commit_hash")
        val DISCORD_CHANNEL_ID = stringPreferencesKey("discord_channel_id")
        val CHECK_INTERVAL_MINUTES = intPreferencesKey("check_interval_minutes")
    }

    val preferences: Flow<FeatureFunnelPreferences> = context.featureFunnelDataStore.data.map { prefs ->
        FeatureFunnelPreferences(
            enabled = prefs[Keys.ENABLED] ?: false,
            lastCheckEpochMillis = prefs[Keys.LAST_CHECK_EPOCH_MILLIS] ?: 0L,
            lastCommitHash = prefs[Keys.LAST_COMMIT_HASH],
            discordChannelId = prefs[Keys.DISCORD_CHANNEL_ID],
            checkIntervalMinutes = prefs[Keys.CHECK_INTERVAL_MINUTES] ?: 30
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.featureFunnelDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setLastCheckTime(epochMillis: Long) {
        context.featureFunnelDataStore.edit { it[Keys.LAST_CHECK_EPOCH_MILLIS] = epochMillis }
    }

    suspend fun setLastCommitHash(hash: String) {
        context.featureFunnelDataStore.edit { it[Keys.LAST_COMMIT_HASH] = hash }
    }

    suspend fun setDiscordChannelId(channelId: String) {
        context.featureFunnelDataStore.edit { it[Keys.DISCORD_CHANNEL_ID] = channelId }
    }
}

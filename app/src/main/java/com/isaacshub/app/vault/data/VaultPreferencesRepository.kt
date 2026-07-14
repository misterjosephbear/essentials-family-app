package com.isaacshub.app.vault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "isaacs_hub_vault_prefs")

private object Keys {
    val BASE_URL = stringPreferencesKey("base_url")
    val API_KEY = stringPreferencesKey("api_key")
    val REMOTE_BASE_URL = stringPreferencesKey("remote_base_url")
    val PREFERRED_BASE_URL = stringPreferencesKey("preferred_base_url")
    val LAST_SYNC_EPOCH_MILLIS = longPreferencesKey("last_sync_epoch_millis")
    val LAST_BACKUP_EPOCH_MILLIS = longPreferencesKey("last_backup_epoch_millis")
}

class VaultPreferencesRepository(private val context: Context) {

    val connection: Flow<VaultConnection?> = context.vaultDataStore.data.map { prefs ->
        val baseUrl = prefs[Keys.BASE_URL]
        val apiKey = prefs[Keys.API_KEY]
        if (baseUrl != null && apiKey != null) {
            VaultConnection(baseUrl, apiKey, prefs[Keys.REMOTE_BASE_URL])
        } else {
            null
        }
    }

    val lastSyncEpochMillis: Flow<Long> = context.vaultDataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_EPOCH_MILLIS] ?: 0L
    }

    val lastBackupEpochMillis: Flow<Long> = context.vaultDataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_EPOCH_MILLIS] ?: 0L
    }

    /**
     * Whichever of [VaultConnection.baseUrl] / [VaultConnection.remoteBaseUrl] last actually
     * answered a request - tried first on the next call so a phone that's usually off the LAN
     * (or usually on it) doesn't pay a connect-timeout tax on every single request.
     */
    val preferredBaseUrl: Flow<String?> = context.vaultDataStore.data.map { prefs -> prefs[Keys.PREFERRED_BASE_URL] }

    suspend fun setConnection(connection: VaultConnection) {
        context.vaultDataStore.edit {
            it[Keys.BASE_URL] = connection.baseUrl
            it[Keys.API_KEY] = connection.apiKey
            if (connection.remoteBaseUrl != null) {
                it[Keys.REMOTE_BASE_URL] = connection.remoteBaseUrl
            } else {
                it.remove(Keys.REMOTE_BASE_URL)
            }
        }
    }

    /** Updates just the remote/tunnel URL for an already-paired connection - lets it be added, changed, or cleared (pass null) without re-pairing the device. */
    suspend fun setRemoteBaseUrl(remoteBaseUrl: String?) {
        context.vaultDataStore.edit {
            if (remoteBaseUrl != null) it[Keys.REMOTE_BASE_URL] = remoteBaseUrl else it.remove(Keys.REMOTE_BASE_URL)
        }
    }

    suspend fun setPreferredBaseUrl(baseUrl: String) {
        context.vaultDataStore.edit { it[Keys.PREFERRED_BASE_URL] = baseUrl }
    }

    suspend fun clearConnection() {
        context.vaultDataStore.edit {
            it.remove(Keys.BASE_URL)
            it.remove(Keys.API_KEY)
            it.remove(Keys.REMOTE_BASE_URL)
            it.remove(Keys.PREFERRED_BASE_URL)
            it.remove(Keys.LAST_SYNC_EPOCH_MILLIS)
            it.remove(Keys.LAST_BACKUP_EPOCH_MILLIS)
        }
    }

    suspend fun setLastSyncEpochMillis(epochMillis: Long) {
        context.vaultDataStore.edit { it[Keys.LAST_SYNC_EPOCH_MILLIS] = epochMillis }
    }

    suspend fun setLastBackupEpochMillis(epochMillis: Long) {
        context.vaultDataStore.edit { it[Keys.LAST_BACKUP_EPOCH_MILLIS] = epochMillis }
    }
}

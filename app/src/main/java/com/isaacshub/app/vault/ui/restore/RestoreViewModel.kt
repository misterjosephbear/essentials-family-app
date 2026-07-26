package com.isaacshub.app.vault.ui.restore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.vault.data.VaultApiClient
import com.isaacshub.app.vault.data.VaultPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

data class BackupDatabase(
    val name: String,
    val displayName: String,
    val fileName: String,
    val selected: Boolean = false
)

sealed class RestoreState {
    object Idle : RestoreState()
    object Loading : RestoreState()
    data class Success(val restoredDatabases: List<String>) : RestoreState()
    data class Error(val message: String) : RestoreState()
}

class RestoreViewModel(
    private val context: Context,
    private val vaultPrefs: VaultPreferencesRepository
) : ViewModel() {

    private val _databases = MutableStateFlow(
        listOf(
            BackupDatabase("app", "App Data (Sleep, Banking, Feature Funnel)", "app.db"),
            BackupDatabase("work", "Work Data (Time Tracking, Route Helper)", "work.db"),
            BackupDatabase("essentials", "Essentials (Chores, Family)", "essentials.db")
        )
    )
    val databases: StateFlow<List<BackupDatabase>> = _databases

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime

    init {
        viewModelScope.launch {
            _lastBackupTime.value = vaultPrefs.lastBackupEpochMillis.first()
        }
    }

    fun toggleDatabaseSelection(databaseName: String) {
        _databases.value = _databases.value.map { db ->
            if (db.name == databaseName) {
                db.copy(selected = !db.selected)
            } else {
                db
            }
        }
    }

    fun restoreSelectedDatabases() {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Loading

            try {
                val connection = vaultPrefs.connection.first()
                if (connection == null) {
                    _restoreState.value = RestoreState.Error("No server connection configured")
                    return@launch
                }

                val client = VaultApiClient(connection, vaultPrefs.preferredBaseUrl.first())
                val selectedDatabases = _databases.value.filter { it.selected }

                if (selectedDatabases.isEmpty()) {
                    _restoreState.value = RestoreState.Error("No databases selected")
                    return@launch
                }

                val restoredList = mutableListOf<String>()
                var anyFailed = false

                for (database in selectedDatabases) {
                    val remotePath = "AppBackup/${database.fileName}"
                    val tempFile = File(context.cacheDir, "restore_${database.fileName}")

                    try {
                        // Download backup from server
                        val success = client.downloadFile(remotePath, tempFile)
                        if (!success) {
                            anyFailed = true
                            continue
                        }

                        // Get the actual database path
                        val dbPath = context.getDatabasePath(database.fileName)

                        // Close any open connections to this database
                        // Note: This requires the app to be restarted to take effect properly
                        context.deleteDatabase(database.fileName)

                        // Copy the downloaded backup to the database location
                        tempFile.copyTo(dbPath, overwrite = true)
                        restoredList.add(database.displayName)

                    } catch (e: Exception) {
                        anyFailed = true
                        android.util.Log.e("RestoreViewModel", "Failed to restore ${database.fileName}", e)
                    } finally {
                        tempFile.delete()
                    }
                }

                if (restoredList.isNotEmpty()) {
                    _restoreState.value = RestoreState.Success(restoredList)
                } else {
                    _restoreState.value = RestoreState.Error("Failed to restore any databases")
                }

            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error(e.message ?: "Unknown error occurred")
                android.util.Log.e("RestoreViewModel", "Restore failed", e)
            }
        }
    }

    fun resetState() {
        _restoreState.value = RestoreState.Idle
    }
}

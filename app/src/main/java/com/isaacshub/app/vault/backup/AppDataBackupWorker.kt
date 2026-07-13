package com.isaacshub.app.vault.backup

import android.content.Context
import androidx.room.RoomDatabase
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.isaacshub.app.App
import com.isaacshub.app.sleep.data.SleepDatabase
import com.isaacshub.app.timetracking.data.TimeTrackingDatabase
import com.isaacshub.app.vault.data.VaultApiClient
import kotlinx.coroutines.flow.first
import java.io.File

private const val REMOTE_FOLDER = "AppBackup"
private val DATABASE_FILE_NAMES = listOf("sleep.db", "time_tracking.db")

/**
 * Backs up every Room database and every app preference to the paired server, so nothing is lost
 * if the app is uninstalled or the phone is lost/replaced. Databases are copied whole (not
 * re-derived field by field) so this stays correct automatically as entities change; preferences
 * are serialized generically for the same reason.
 */
class AppDataBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val vaultPrefs = app.vaultPreferencesRepository
        val connection = vaultPrefs.connection.first() ?: return Result.success()
        val client = VaultApiClient(connection)

        checkpointWal(SleepDatabase.getInstance(applicationContext))
        checkpointWal(TimeTrackingDatabase.getInstance(applicationContext))

        var allSucceeded = true
        for (dbName in DATABASE_FILE_NAMES) {
            val dbFile = applicationContext.getDatabasePath(dbName)
            if (!dbFile.exists()) continue
            if (!client.uploadFile(dbFile, "$REMOTE_FOLDER/$dbName", "application/octet-stream")) {
                allSucceeded = false
            }
        }

        val tempPrefsFile = File(applicationContext.cacheDir, "preferences_backup.json")
        try {
            val json = preferencesToJson(app.preferencesRepository.rawPreferences.first())
            tempPrefsFile.writeText(json)
            if (!client.uploadFile(tempPrefsFile, "$REMOTE_FOLDER/preferences.json", "application/json")) {
                allSucceeded = false
            }
        } finally {
            tempPrefsFile.delete()
        }

        if (allSucceeded) vaultPrefs.setLastBackupEpochMillis(System.currentTimeMillis())

        return if (allSucceeded) Result.success() else Result.retry()
    }

    /** Flushes WAL into the main db file so the copy we upload isn't missing recently-committed writes. */
    private fun checkpointWal(database: RoomDatabase) {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        }
    }
}

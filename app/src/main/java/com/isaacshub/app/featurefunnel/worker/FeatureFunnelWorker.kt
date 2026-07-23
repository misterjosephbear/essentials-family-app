package com.isaacshub.app.featurefunnel.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.isaacshub.app.App
import com.isaacshub.app.featurefunnel.data.*
import kotlinx.coroutines.flow.first

private const val TAG = "FeatureFunnelWorker"

/**
 * Background worker that runs every 30 minutes to:
 * 1. Check if there's a queued prompt to send
 * 2. Try sending it to Discord (via Claude Code channel)
 * 3. Monitor git commits for completion
 */
class FeatureFunnelWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val vaultPrefs = app.vaultPreferencesRepository
        val connection = vaultPrefs.connection.first()

        if (connection == null) {
            Log.d(TAG, "No vault connection - skipping feature funnel check")
            return Result.success()
        }

        val funnelPrefs = app.featureFunnelPreferencesRepository
        val prefs = funnelPrefs.preferences.first()

        if (!prefs.enabled) {
            Log.d(TAG, "Feature funnel disabled - skipping")
            return Result.success()
        }

        val channelId = prefs.discordChannelId
        if (channelId == null) {
            Log.d(TAG, "No Discord channel configured - skipping")
            return Result.success()
        }

        val repository = app.featureFunnelRepository
        val apiClient = FeatureFunnelApiClient(connection)

        // Step 1: Check for completion of in-progress prompt
        val inProgress = repository.getCurrentInProgress()
        if (inProgress != null) {
            val completed = checkForCompletion(apiClient, repository, funnelPrefs, prefs, inProgress)
            if (completed) {
                // Notify Discord
                apiClient.notifyCompletion(
                    channelId = channelId,
                    promptTitle = inProgress.title,
                    commitHash = prefs.lastCommitHash ?: "unknown"
                )
                Log.d(TAG, "Prompt completed: ${inProgress.title}")
            } else {
                // Still in progress, don't send another prompt yet
                Log.d(TAG, "Prompt still in progress: ${inProgress.title}")
                funnelPrefs.setLastCheckTime(System.currentTimeMillis())
                return Result.success()
            }
        }

        // Step 2: Send next queued prompt if available
        val nextPrompt = repository.getNextPromptToSend()
        if (nextPrompt != null) {
            Log.d(TAG, "Sending prompt: ${nextPrompt.title}")
            when (val result = apiClient.sendPromptToDiscord(channelId, nextPrompt.promptText)) {
                is SendPromptResult.Success -> {
                    repository.markPromptSent(nextPrompt.id)
                    Log.d(TAG, "Prompt sent successfully")
                }
                is SendPromptResult.LimitHit -> {
                    Log.d(TAG, "Claude usage limit hit - will retry later")
                    // Don't mark as failed, just wait for next cycle
                }
                is SendPromptResult.Failed -> {
                    Log.e(TAG, "Failed to send prompt: ${result.message}")
                    repository.markPromptFailed(nextPrompt.id, result.message)
                }
            }
        } else {
            Log.d(TAG, "No queued prompts to send")
        }

        // Update last check time
        funnelPrefs.setLastCheckTime(System.currentTimeMillis())

        return Result.success()
    }

    /**
     * Checks if a new git commit has appeared since we sent the prompt.
     * Returns true if completed, false if still in progress.
     */
    private suspend fun checkForCompletion(
        apiClient: FeatureFunnelApiClient,
        repository: FeatureFunnelRepository,
        prefsRepository: FeatureFunnelPreferencesRepository,
        prefs: FeatureFunnelPreferences,
        prompt: FeaturePromptEntity
    ): Boolean {
        val currentHash = apiClient.getLatestCommitHash() ?: return false
        val previousHash = prefs.lastCommitHash

        // If hash changed since we sent the prompt, assume it's complete
        val hasNewCommit = previousHash != null && currentHash != previousHash

        if (hasNewCommit) {
            repository.markPromptCompleted(prompt.id)
            prefsRepository.setLastCommitHash(currentHash)
            return true
        }

        return false
    }
}

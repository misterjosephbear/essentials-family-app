package com.isaacshub.essentials.data.api

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ChoreDto(
    val id: Long,
    val name: String,
    val description: String,
    val photoRequirement: String?,
    val daysOfWeek: List<String>
)

@Serializable
data class ChoresResponse(
    val chores: List<ChoreDto>
)

@Serializable
data class PhotoVerificationRequest(
    val completionId: Long,
    val choreId: Long,
    val photoBase64: String,
    val photoRequirement: String
)

@Serializable
data class PhotoVerificationResponse(
    val verified: Boolean,
    val feedback: String
)

@Serializable
data class CompletionSyncRequest(
    val choreId: Long,
    val completionDate: String,
    val photoUri: String?,
    val completedAtEpochMillis: Long
)

@Serializable
data class CompletionSyncResponse(
    val success: Boolean,
    val completionId: Long
)

class EssentialsApiClient(
    private val serverUrl: String,
    private val authToken: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "EssentialsApiClient"
    }

    /**
     * Fetch chores assigned to the current child user
     */
    suspend fun fetchChores(): Result<List<ChoreDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$serverUrl/api/essentials/chores/assigned")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $authToken")
            conn.setRequestProperty("Content-Type", "application/json")

            if (conn.responseCode != 200) {
                throw Exception("Failed to fetch chores: HTTP ${conn.responseCode}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val choresResponse = json.decodeFromString<ChoresResponse>(response)

            Log.d(TAG, "Fetched ${choresResponse.chores.size} chores")
            choresResponse.chores
        }
    }

    /**
     * Submit a photo for AI verification via the server's Claude API
     */
    suspend fun verifyPhoto(
        completionId: Long,
        choreId: Long,
        photoFile: File,
        photoRequirement: String
    ): Result<PhotoVerificationResponse> = withContext(Dispatchers.IO) {
        runCatching {
            // Read and encode photo
            val photoBytes = photoFile.readBytes()
            val photoBase64 = Base64.encodeToString(photoBytes, Base64.NO_WRAP)

            val url = URL("$serverUrl/api/essentials/completions/verify-photo")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $authToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val request = PhotoVerificationRequest(
                completionId = completionId,
                choreId = choreId,
                photoBase64 = photoBase64,
                photoRequirement = photoRequirement
            )

            val requestBody = kotlinx.serialization.json.Json.encodeToString(PhotoVerificationRequest.serializer(), request)
            conn.outputStream.use { it.write(requestBody.toByteArray()) }

            if (conn.responseCode != 200) {
                throw Exception("Photo verification failed: HTTP ${conn.responseCode}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val result = json.decodeFromString<PhotoVerificationResponse>(response)

            Log.d(TAG, "Photo verification result: verified=${result.verified}")
            result
        }
    }

    /**
     * Sync a completion to the server
     */
    suspend fun syncCompletion(
        choreId: Long,
        completionDate: String,
        photoUri: String?,
        completedAtEpochMillis: Long
    ): Result<CompletionSyncResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$serverUrl/api/essentials/completions/sync")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $authToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val request = CompletionSyncRequest(
                choreId = choreId,
                completionDate = completionDate,
                photoUri = photoUri,
                completedAtEpochMillis = completedAtEpochMillis
            )

            val requestBody = kotlinx.serialization.json.Json.encodeToString(CompletionSyncRequest.serializer(), request)
            conn.outputStream.use { it.write(requestBody.toByteArray()) }

            if (conn.responseCode != 200) {
                throw Exception("Completion sync failed: HTTP ${conn.responseCode}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val result = json.decodeFromString<CompletionSyncResponse>(response)

            Log.d(TAG, "Completion synced: success=${result.success}")
            result
        }
    }
}

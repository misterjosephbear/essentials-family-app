package com.isaacshub.app.essentials.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP client for communicating with Isaac's Hub Server Essentials API.
 * Follows the same pattern as VaultApiClient using HttpURLConnection.
 */
class EssentialsApiClient(
    private val baseUrl: String,
    private val apiKey: String
) {

    private suspend fun <T> httpRequest(
        endpoint: String,
        method: String = "GET",
        body: JSONObject? = null,
        parseResponse: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("$baseUrl$endpoint").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000

            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }

            try {
                val responseCode = conn.responseCode
                val responseText = if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (responseCode !in 200..299) {
                    throw ApiException(responseCode, responseText)
                }

                parseResponse(responseText)
            } finally {
                conn.disconnect()
            }
        }
    }

    // ============================================================================
    // Child Account Management
    // ============================================================================

    suspend fun getChildren(): Result<List<ChildAccountDto>> = httpRequest(
        endpoint = "/api/essentials/children",
        method = "GET"
    ) { response ->
        val json = JSONArray(response)
        List(json.length()) { i ->
            val obj = json.getJSONObject(i)
            ChildAccountDto(
                id = obj.getLong("id"),
                username = obj.getString("username"),
                displayName = obj.getString("displayName"),
                createdAt = obj.getLong("createdAt")
            )
        }
    }

    suspend fun createChild(
        username: String,
        displayName: String,
        password: String
    ): Result<ChildAccountDto> = httpRequest(
        endpoint = "/api/essentials/children",
        method = "POST",
        body = JSONObject().apply {
            put("username", username)
            put("displayName", displayName)
            put("password", password)
        }
    ) { response ->
        val obj = JSONObject(response)
        ChildAccountDto(
            id = obj.getLong("id"),
            username = obj.getString("username"),
            displayName = obj.getString("displayName"),
            createdAt = obj.getLong("createdAt")
        )
    }

    suspend fun updateChild(
        id: Long,
        username: String,
        displayName: String,
        password: String? = null
    ): Result<Unit> = httpRequest(
        endpoint = "/api/essentials/children/$id",
        method = "PUT",
        body = JSONObject().apply {
            put("username", username)
            put("displayName", displayName)
            if (password != null) put("password", password)
        }
    ) { Unit }

    suspend fun deleteChild(id: Long): Result<Unit> = httpRequest(
        endpoint = "/api/essentials/children/$id",
        method = "DELETE"
    ) { Unit }

    // ============================================================================
    // Chore Management
    // ============================================================================

    suspend fun getChores(): Result<List<ChoreDto>> = httpRequest(
        endpoint = "/api/essentials/chores",
        method = "GET"
    ) { response ->
        val json = JSONArray(response)
        List(json.length()) { i ->
            val obj = json.getJSONObject(i)
            ChoreDto(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                description = obj.getString("description"),
                photoRequirement = obj.optString("photoRequirement").takeIf { it.isNotEmpty() },
                daysOfWeek = parseDaysOfWeek(obj.getJSONArray("daysOfWeek")),
                assignedChildIds = parseChildIds(obj.getJSONArray("assignedChildIds")),
                createdAt = obj.getLong("createdAt")
            )
        }
    }

    suspend fun createChore(
        name: String,
        description: String,
        photoRequirement: String?,
        daysOfWeek: List<String>,
        assignedChildIds: List<Long>
    ): Result<ChoreDto> = httpRequest(
        endpoint = "/api/essentials/chores",
        method = "POST",
        body = JSONObject().apply {
            put("name", name)
            put("description", description)
            if (photoRequirement != null) put("photoRequirement", photoRequirement)
            put("daysOfWeek", JSONArray(daysOfWeek))
            put("assignedChildIds", JSONArray(assignedChildIds))
        }
    ) { response ->
        val obj = JSONObject(response)
        ChoreDto(
            id = obj.getLong("id"),
            name = obj.getString("name"),
            description = obj.getString("description"),
            photoRequirement = obj.optString("photoRequirement").takeIf { it.isNotEmpty() },
            daysOfWeek = parseDaysOfWeek(obj.getJSONArray("daysOfWeek")),
            assignedChildIds = parseChildIds(obj.getJSONArray("assignedChildIds")),
            createdAt = obj.getLong("createdAt")
        )
    }

    suspend fun updateChore(
        id: Long,
        name: String,
        description: String,
        photoRequirement: String?,
        daysOfWeek: List<String>,
        assignedChildIds: List<Long>
    ): Result<Unit> = httpRequest(
        endpoint = "/api/essentials/chores/$id",
        method = "PUT",
        body = JSONObject().apply {
            put("name", name)
            put("description", description)
            if (photoRequirement != null) put("photoRequirement", photoRequirement)
            put("daysOfWeek", JSONArray(daysOfWeek))
            put("assignedChildIds", JSONArray(assignedChildIds))
        }
    ) { Unit }

    suspend fun deleteChore(id: Long): Result<Unit> = httpRequest(
        endpoint = "/api/essentials/chores/$id",
        method = "DELETE"
    ) { Unit }

    // ============================================================================
    // Helper Functions
    // ============================================================================

    private fun parseDaysOfWeek(jsonArray: JSONArray): List<String> {
        return List(jsonArray.length()) { jsonArray.getString(it) }
    }

    private fun parseChildIds(jsonArray: JSONArray): List<Long> {
        return List(jsonArray.length()) { jsonArray.getLong(it) }
    }
}

// ============================================================================
// DTOs (Data Transfer Objects)
// ============================================================================

data class ChildAccountDto(
    val id: Long,
    val username: String,
    val displayName: String,
    val createdAt: Long
)

data class ChoreDto(
    val id: Long,
    val name: String,
    val description: String,
    val photoRequirement: String?,
    val daysOfWeek: List<String>,
    val assignedChildIds: List<Long>,
    val createdAt: Long
)

class ApiException(val code: Int, message: String) : Exception("HTTP $code: $message")

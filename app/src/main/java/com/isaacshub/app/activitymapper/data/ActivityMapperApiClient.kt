package com.isaacshub.app.activitymapper.data

import com.isaacshub.app.core.network.BaseApiClient
import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * API client for Activity Mapper automation system.
 * Communicates with isaacs-hub-server's /api/activity-mapper endpoints.
 */
class ActivityMapperApiClient(connection: VaultConnection) : BaseApiClient(connection) {

    // ==================== Rules ====================

    suspend fun getRules(): Result<List<AutomationRule>> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val json = get(baseUrl, "/api/activity-mapper/rules")
            val rulesArray = json.getJSONArray("rules")
            List(rulesArray.length()) { i ->
                parseAutomationRule(rulesArray.getJSONObject(i))
            }
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    suspend fun createRule(rule: AutomationRule): Result<AutomationRule> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val body = serializeAutomationRule(rule)
            val json = post(baseUrl, "/api/activity-mapper/rules", body)
            parseAutomationRule(json)
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    suspend fun updateRule(rule: AutomationRule): Result<AutomationRule> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val body = serializeAutomationRule(rule)
            val json = put(baseUrl, "/api/activity-mapper/rules/${rule.id}", body)
            parseAutomationRule(json)
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    suspend fun deleteRule(ruleId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            delete(baseUrl, "/api/activity-mapper/rules/$ruleId")
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    // ==================== Rich Presence Profiles ====================

    suspend fun getRichPresenceProfiles(): Result<List<DiscordRichPresenceProfile>> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val json = get(baseUrl, "/api/activity-mapper/rich-presence-profiles")
            val profilesArray = json.getJSONArray("profiles")
            List(profilesArray.length()) { i ->
                parseRichPresenceProfile(profilesArray.getJSONObject(i))
            }
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    suspend fun createRichPresenceProfile(profile: DiscordRichPresenceProfile): Result<DiscordRichPresenceProfile> =
        withContext(Dispatchers.IO) {
            val result = tryEachBaseUrl { baseUrl ->
                val body = serializeRichPresenceProfile(profile)
                val json = post(baseUrl, "/api/activity-mapper/rich-presence-profiles", body)
                parseRichPresenceProfile(json)
            }
            result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
        }

    suspend fun updateRichPresenceProfile(profile: DiscordRichPresenceProfile): Result<DiscordRichPresenceProfile> =
        withContext(Dispatchers.IO) {
            val result = tryEachBaseUrl { baseUrl ->
                val body = serializeRichPresenceProfile(profile)
                val json = put(baseUrl, "/api/activity-mapper/rich-presence-profiles/${profile.id}", body)
                parseRichPresenceProfile(json)
            }
            result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
        }

    suspend fun deleteRichPresenceProfile(profileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            delete(baseUrl, "/api/activity-mapper/rich-presence-profiles/$profileId")
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    // ==================== Variables ====================

    suspend fun getVariables(): Result<List<VariableValue>> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val json = get(baseUrl, "/api/activity-mapper/variables")
            val variablesArray = json.getJSONArray("variables")
            List(variablesArray.length()) { i ->
                parseVariableValue(variablesArray.getJSONObject(i))
            }
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    suspend fun setVariable(type: VariableType, value: Any): Result<VariableValue> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val body = JSONObject().apply {
                put("value", value)
            }
            val json = post(baseUrl, "/api/activity-mapper/variables/${type.name}", body)
            parseVariableValue(json)
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    // ==================== Parsing ====================

    private fun parseVariableValue(json: JSONObject): VariableValue {
        return VariableValue(
            type = VariableType.valueOf(json.getString("type")),
            value = json.get("value"),
            lastUpdated = json.getLong("lastUpdated")
        )
    }

    private fun parseCondition(json: JSONObject): Condition {
        return Condition(
            variable = VariableType.valueOf(json.getString("variable")),
            operator = ComparisonOperator.fromSymbol(json.getString("operator")),
            value = json.get("value")
        )
    }

    private fun parseAction(json: JSONObject): Action {
        return when (ActionType.valueOf(json.getString("type"))) {
            ActionType.SET_DISCORD_RICH_PRESENCE -> Action.SetDiscordRichPresence(
                profileId = json.getString("profileId")
            )
        }
    }

    private fun parseAutomationRule(json: JSONObject): AutomationRule {
        val conditionsArray = json.getJSONArray("conditions")
        val actionsArray = json.getJSONArray("actions")

        return AutomationRule(
            id = json.getInt("id"),
            name = json.getString("name"),
            enabled = json.getBoolean("enabled"),
            priority = json.getInt("priority"),
            conditions = List(conditionsArray.length()) { i ->
                parseCondition(conditionsArray.getJSONObject(i))
            },
            logicalOperator = LogicalOperator.valueOf(json.getString("logicalOperator")),
            actions = List(actionsArray.length()) { i ->
                parseAction(actionsArray.getJSONObject(i))
            }
        )
    }

    private fun parseRichPresenceProfile(json: JSONObject): DiscordRichPresenceProfile {
        return DiscordRichPresenceProfile(
            id = json.getString("id"),
            name = json.getString("name"),
            state = json.optString("state").takeIf { it.isNotBlank() },
            details = json.optString("details").takeIf { it.isNotBlank() },
            largeImageKey = json.optString("largeImageKey").takeIf { it.isNotBlank() },
            largeImageText = json.optString("largeImageText").takeIf { it.isNotBlank() },
            smallImageKey = json.optString("smallImageKey").takeIf { it.isNotBlank() },
            smallImageText = json.optString("smallImageText").takeIf { it.isNotBlank() }
        )
    }

    // ==================== Serialization ====================

    private fun serializeCondition(condition: Condition): JSONObject {
        return JSONObject().apply {
            put("variable", condition.variable.name)
            put("operator", condition.operator.symbol)
            put("value", condition.value)
        }
    }

    private fun serializeAction(action: Action): JSONObject {
        return when (action) {
            is Action.SetDiscordRichPresence -> JSONObject().apply {
                put("type", "SET_DISCORD_RICH_PRESENCE")
                put("profileId", action.profileId)
            }
        }
    }

    private fun serializeAutomationRule(rule: AutomationRule): JSONObject {
        return JSONObject().apply {
            put("id", rule.id)
            put("name", rule.name)
            put("enabled", rule.enabled)
            put("priority", rule.priority)
            put("logicalOperator", rule.logicalOperator.name)
            put("conditions", JSONArray().apply {
                rule.conditions.forEach { put(serializeCondition(it)) }
            })
            put("actions", JSONArray().apply {
                rule.actions.forEach { put(serializeAction(it)) }
            })
        }
    }

    private fun serializeRichPresenceProfile(profile: DiscordRichPresenceProfile): JSONObject {
        return JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            profile.state?.let { put("state", it) }
            profile.details?.let { put("details", it) }
            profile.largeImageKey?.let { put("largeImageKey", it) }
            profile.largeImageText?.let { put("largeImageText", it) }
            profile.smallImageKey?.let { put("smallImageKey", it) }
            profile.smallImageText?.let { put("smallImageText", it) }
        }
    }
}

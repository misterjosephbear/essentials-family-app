package com.isaacshub.app.activitymapper.data

/**
 * Data models for the Activity Mapper feature - automation system that monitors variables
 * and triggers actions based on conditions.
 *
 * These models match the server-side TypeScript types from activity-mapper/types.ts
 */

// ==================== Variables ====================

enum class VariableType {
    ROUTE_PLAY_MODE_ACTIVE,
    ROUTE_COMPLETION_PERCENT,
    SLEEP_TIME_REMAINING,
    SLEEPING,
    CURRENT_STOP_NUMBER,
    TOTAL_STOPS
}

data class VariableValue(
    val type: VariableType,
    val value: Any, // Can be Boolean, Number, or String
    val lastUpdated: Long // epoch millis
)

// ==================== Conditions ====================

enum class ComparisonOperator(val symbol: String) {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<=");

    companion object {
        fun fromSymbol(symbol: String): ComparisonOperator = values().first { it.symbol == symbol }
    }
}

data class Condition(
    val variable: VariableType,
    val operator: ComparisonOperator,
    val value: Any // Can be Boolean, Number, or String
)

// ==================== Actions ====================

enum class ActionType {
    SET_DISCORD_RICH_PRESENCE
}

sealed class Action {
    abstract val type: ActionType

    data class SetDiscordRichPresence(
        val profileId: String
    ) : Action() {
        override val type: ActionType = ActionType.SET_DISCORD_RICH_PRESENCE
    }
}

// ==================== Discord Rich Presence Profiles ====================

data class DiscordRichPresenceProfile(
    val id: String,
    val name: String, // e.g., "Delivering Mail", "Idle", "Sleeping"
    val state: String? = null, // Bottom text, supports variables like "Stop {{CURRENT_STOP_NUMBER}}/{{TOTAL_STOPS}}"
    val details: String? = null, // Top text, supports variables
    val largeImageKey: String? = null,
    val largeImageText: String? = null,
    val smallImageKey: String? = null,
    val smallImageText: String? = null
)

// ==================== Rules ====================

enum class LogicalOperator {
    AND,
    OR
}

data class AutomationRule(
    val id: Int,
    val name: String,
    val enabled: Boolean,
    val priority: Int, // Higher priority rules are evaluated first (for conflicting actions)
    val conditions: List<Condition>,
    val logicalOperator: LogicalOperator, // How to combine multiple conditions
    val actions: List<Action>
)

// ==================== Storage Format ====================

data class ActivityMapperData(
    val rules: List<AutomationRule>,
    val richPresenceProfiles: List<DiscordRichPresenceProfile>,
    val nextRuleId: Int
)

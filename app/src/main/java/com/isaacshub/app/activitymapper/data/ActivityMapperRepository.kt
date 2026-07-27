package com.isaacshub.app.activitymapper.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for Activity Mapper automation system.
 * Manages rules, Rich Presence profiles, and variable values.
 */
class ActivityMapperRepository(private val apiClient: ActivityMapperApiClient) {

    private val _rules = MutableStateFlow<List<AutomationRule>>(emptyList())
    val rules: StateFlow<List<AutomationRule>> = _rules.asStateFlow()

    private val _richPresenceProfiles = MutableStateFlow<List<DiscordRichPresenceProfile>>(emptyList())
    val richPresenceProfiles: StateFlow<List<DiscordRichPresenceProfile>> = _richPresenceProfiles.asStateFlow()

    private val _variables = MutableStateFlow<List<VariableValue>>(emptyList())
    val variables: StateFlow<List<VariableValue>> = _variables.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ==================== Rules ====================

    suspend fun loadRules(): Result<Unit> {
        _isLoading.value = true
        _error.value = null
        return try {
            val result = apiClient.getRules()
            result.fold(
                onSuccess = { rulesList ->
                    _rules.value = rulesList.sortedByDescending { it.priority }
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    Result.failure(exception)
                }
            )
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createRule(rule: AutomationRule): Result<AutomationRule> {
        _error.value = null
        return apiClient.createRule(rule).also { result ->
            result.onSuccess {
                loadRules()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    suspend fun updateRule(rule: AutomationRule): Result<AutomationRule> {
        _error.value = null
        return apiClient.updateRule(rule).also { result ->
            result.onSuccess {
                loadRules()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    suspend fun deleteRule(ruleId: Int): Result<Boolean> {
        _error.value = null
        return apiClient.deleteRule(ruleId).also { result ->
            result.onSuccess {
                loadRules()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    suspend fun toggleRuleEnabled(rule: AutomationRule): Result<AutomationRule> {
        return updateRule(rule.copy(enabled = !rule.enabled))
    }

    // ==================== Rich Presence Profiles ====================

    suspend fun loadRichPresenceProfiles(): Result<Unit> {
        _isLoading.value = true
        _error.value = null
        return try {
            val result = apiClient.getRichPresenceProfiles()
            result.fold(
                onSuccess = { profilesList ->
                    _richPresenceProfiles.value = profilesList
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    Result.failure(exception)
                }
            )
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createRichPresenceProfile(profile: DiscordRichPresenceProfile): Result<DiscordRichPresenceProfile> {
        _error.value = null
        return apiClient.createRichPresenceProfile(profile).also { result ->
            result.onSuccess {
                loadRichPresenceProfiles()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    suspend fun updateRichPresenceProfile(profile: DiscordRichPresenceProfile): Result<DiscordRichPresenceProfile> {
        _error.value = null
        return apiClient.updateRichPresenceProfile(profile).also { result ->
            result.onSuccess {
                loadRichPresenceProfiles()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    suspend fun deleteRichPresenceProfile(profileId: String): Result<Boolean> {
        _error.value = null
        return apiClient.deleteRichPresenceProfile(profileId).also { result ->
            result.onSuccess {
                loadRichPresenceProfiles()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    // ==================== Variables ====================

    suspend fun loadVariables(): Result<Unit> {
        _isLoading.value = true
        _error.value = null
        return try {
            val result = apiClient.getVariables()
            result.fold(
                onSuccess = { variablesList ->
                    _variables.value = variablesList
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    Result.failure(exception)
                }
            )
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun setVariable(type: VariableType, value: Any): Result<VariableValue> {
        _error.value = null
        return apiClient.setVariable(type, value).also { result ->
            result.onSuccess {
                loadVariables()
            }
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    suspend fun refresh() {
        loadRules()
        loadRichPresenceProfiles()
        loadVariables()
    }

    fun clearError() {
        _error.value = null
    }
}

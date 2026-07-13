package com.isaacshub.app.timetracking.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.app.core.data.prefs.UserPreferencesRepository
import com.isaacshub.app.timetracking.data.DeductionEntity
import com.isaacshub.app.timetracking.data.DeductionType
import com.isaacshub.app.timetracking.data.TimeTrackingRepository
import com.isaacshub.app.timetracking.ui.formatNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimeTrackerSettingsUiState(
    val hourlyRate: String = "",
    val overtimeMultiplier: String = "",
    val emaRate: String = "",
    val error: String? = null,
    val justSaved: Boolean = false
)

data class DeductionFormState(
    val name: String = "",
    val type: DeductionType = DeductionType.FLAT,
    val amount: String = "",
    val error: String? = null
)

class TimeTrackerSettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val repository: TimeTrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeTrackerSettingsUiState())
    val uiState: StateFlow<TimeTrackerSettingsUiState> = _uiState.asStateFlow()

    private val _deductionForm = MutableStateFlow(DeductionFormState())
    val deductionForm: StateFlow<DeductionFormState> = _deductionForm.asStateFlow()

    val deductions: StateFlow<List<DeductionEntity>> = repository.observeDeductions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            _uiState.value = TimeTrackerSettingsUiState(
                hourlyRate = formatNumber(prefs.hourlyRate),
                overtimeMultiplier = formatNumber(prefs.overtimeMultiplier),
                emaRate = formatNumber(prefs.emaRate)
            )
        }
    }

    fun setHourlyRate(value: String) {
        _uiState.value = _uiState.value.copy(hourlyRate = value, error = null, justSaved = false)
    }

    fun setOvertimeMultiplier(value: String) {
        _uiState.value = _uiState.value.copy(overtimeMultiplier = value, error = null, justSaved = false)
    }

    fun setEmaRate(value: String) {
        _uiState.value = _uiState.value.copy(emaRate = value, error = null, justSaved = false)
    }

    fun save() {
        val state = _uiState.value
        val hourly = state.hourlyRate.toDoubleOrNull()
        val overtime = state.overtimeMultiplier.toDoubleOrNull()
        val ema = state.emaRate.toDoubleOrNull()
        when {
            hourly == null || hourly < 0 -> _uiState.value = state.copy(error = "Enter a valid hourly rate")
            overtime == null || overtime < 1 ->
                _uiState.value = state.copy(error = "Enter a valid overtime multiplier (e.g. 1.5)")
            ema == null || ema < 0 -> _uiState.value = state.copy(error = "Enter a valid EMA rate")
            else -> viewModelScope.launch {
                preferencesRepository.setTimeTrackingRates(hourly, overtime, ema)
                _uiState.value = _uiState.value.copy(justSaved = true)
            }
        }
    }

    fun setDeductionName(value: String) {
        _deductionForm.value = _deductionForm.value.copy(name = value, error = null)
    }

    fun setDeductionType(value: DeductionType) {
        _deductionForm.value = _deductionForm.value.copy(type = value, error = null)
    }

    fun setDeductionAmount(value: String) {
        _deductionForm.value = _deductionForm.value.copy(amount = value, error = null)
    }

    fun addDeduction() {
        val form = _deductionForm.value
        val amount = form.amount.toDoubleOrNull()
        when {
            form.name.isBlank() -> _deductionForm.value = form.copy(error = "Enter a name")
            amount == null || amount < 0 -> _deductionForm.value = form.copy(error = "Enter a valid amount")
            else -> viewModelScope.launch {
                repository.addDeduction(DeductionEntity(name = form.name.trim(), type = form.type, amount = amount))
                _deductionForm.value = DeductionFormState()
            }
        }
    }

    fun deleteDeduction(deduction: DeductionEntity) {
        viewModelScope.launch { repository.deleteDeduction(deduction) }
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository,
        private val repository: TimeTrackingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TimeTrackerSettingsViewModel(preferencesRepository, repository) as T
    }
}

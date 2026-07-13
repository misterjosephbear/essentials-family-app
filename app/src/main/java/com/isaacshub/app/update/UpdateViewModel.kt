package com.isaacshub.app.update

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val availableRelease: ReleaseInfo? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val lastCheckFailure: String? = null
)

@Suppress("DEPRECATION")
private fun currentVersionCode(context: Context): Long {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val installedVersionCode = runCatching { currentVersionCode(getApplication()) }.getOrDefault(Long.MAX_VALUE)
            when (val result = UpdateChecker.fetchLatestRelease()) {
                is UpdateCheckResult.Success -> {
                    if (result.release.versionCode > installedVersionCode) {
                        _uiState.value = _uiState.value.copy(availableRelease = result.release)
                    }
                }
                is UpdateCheckResult.Failure -> {
                    _uiState.value = _uiState.value.copy(lastCheckFailure = result.reason)
                }
            }
        }
    }

    fun installUpdate() {
        val release = _uiState.value.availableRelease ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(downloading = true, error = null, progress = 0f)
            runCatching {
                UpdateInstaller.downloadAndInstall(getApplication(), release) { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(downloading = false, error = "Couldn't download the update. Try again later.")
            }.onSuccess {
                _uiState.value = _uiState.value.copy(downloading = false)
            }
        }
    }

    fun dismiss() {
        _uiState.value = _uiState.value.copy(availableRelease = null, lastCheckFailure = null)
    }
}

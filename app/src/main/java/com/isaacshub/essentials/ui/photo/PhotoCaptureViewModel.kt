package com.isaacshub.essentials.ui.photo

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.isaacshub.essentials.data.repository.EssentialsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class PhotoCaptureUiState(
    val capturedPhotoUri: Uri? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class PhotoCaptureViewModel(
    private val choreId: Long,
    private val essentialsRepository: EssentialsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoCaptureUiState())
    val uiState: StateFlow<PhotoCaptureUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()

    fun onPhotoCaptured(uri: Uri) {
        _uiState.update { it.copy(capturedPhotoUri = uri) }
    }

    fun retakePhoto() {
        _uiState.update { it.copy(capturedPhotoUri = null) }
    }

    fun submitPhoto(onSuccess: () -> Unit) {
        val photoUri = _uiState.value.capturedPhotoUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            try {
                essentialsRepository.markChoreCompleted(
                    choreId = choreId,
                    date = today,
                    photoUri = photoUri.toString()
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to submit photo: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun createImageFile(context: Context): File {
        val timeStamp = System.currentTimeMillis()
        val storageDir = context.getExternalFilesDir(null)
        return File.createTempFile(
            "CHORE_${choreId}_${timeStamp}",
            ".jpg",
            storageDir
        )
    }

    class Factory(
        private val choreId: Long,
        private val essentialsRepository: EssentialsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PhotoCaptureViewModel(choreId, essentialsRepository) as T
        }
    }
}

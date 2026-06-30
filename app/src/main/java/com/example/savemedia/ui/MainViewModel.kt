package com.example.savemedia.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savemedia.domain.MediaEntity
import com.example.savemedia.usecases.GetSavedMediaUseCase
import com.example.savemedia.usecases.SaveMediaUseCase
import com.example.savemedia.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = false,
    val lastSaved: MediaEntity? = null,
    val savedCount: Int = 0,
    val error: String? = null,
    val recentMedia: List<MediaEntity> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val saveMediaUseCase: SaveMediaUseCase,
    private val getSavedMediaUseCase: GetSavedMediaUseCase,
    private val logger: AppLogger
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getSavedMediaUseCase().collect { media ->
                _state.update { it.copy(recentMedia = media, savedCount = media.size) }
            }
        }
    }

    fun onMediaDetected(bitmap: Bitmap, appName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            logger.i("Media detected in ViewModel", "MainViewModel", mapOf("app" to appName))

            saveMediaUseCase(bitmap, appName).fold(
                onSuccess = { media ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            lastSaved = media,
                            savedCount = state.savedCount + 1,
                            error = null
                        )
                    }
                    logger.i("Media saved successfully", "MainViewModel")
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                    logger.e("Failed to save media", error, "MainViewModel")
                }
            )
        }
    }
}

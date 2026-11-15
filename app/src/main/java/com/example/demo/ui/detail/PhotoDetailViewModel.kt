package com.example.demo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.domain.usecase.GetPhotoDetailsUseCase
import com.example.demo.ui.common.toErrorMessageRes
import com.example.demo.ui.navigation.PhotoDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotoDetails: GetPhotoDetailsUseCase,
) : ViewModel() {

    /**
     * Navigation arguments are placed in the SavedStateHandle by Navigation Compose,
     * keyed by the route's property name. This survives process death and is trivial
     * to provide in unit tests.
     */
    private val photoId: String = checkNotNull(savedStateHandle[PhotoDetailDestination.ARG_PHOTO_ID])

    private val _state = MutableStateFlow(PhotoDetailState())
    val state: StateFlow<PhotoDetailState> = _state.asStateFlow()

    private val _effects = Channel<PhotoDetailEffect>(Channel.BUFFERED)
    val effects: Flow<PhotoDetailEffect> = _effects.receiveAsFlow()

    init {
        loadPhoto()
    }

    fun onIntent(intent: PhotoDetailIntent) {
        when (intent) {
            PhotoDetailIntent.Retry -> loadPhoto()
            PhotoDetailIntent.BackClicked -> sendEffect(PhotoDetailEffect.NavigateBack)
            PhotoDetailIntent.OpenSourceClicked ->
                _state.value.photo?.let { sendEffect(PhotoDetailEffect.OpenUrl(it.sourceUrl)) }
        }
    }

    private fun loadPhoto() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getPhotoDetails(photoId)
                .onSuccess { photo -> _state.update { it.copy(photo = photo, isLoading = false) } }
                .onFailure { error ->
                    _state.update { it.copy(error = error.toErrorMessageRes(), isLoading = false) }
                }
        }
    }

    private fun sendEffect(effect: PhotoDetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

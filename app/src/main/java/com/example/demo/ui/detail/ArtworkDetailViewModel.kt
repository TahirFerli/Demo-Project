package com.example.demo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.domain.usecase.GetArtworkDetailsUseCase
import com.example.demo.ui.common.toErrorMessageRes
import com.example.demo.ui.navigation.ArtworkDetailDestination
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
class ArtworkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArtworkDetails: GetArtworkDetailsUseCase,
) : ViewModel() {

    /**
     * Navigation arguments are placed in the SavedStateHandle by Navigation Compose,
     * keyed by the route's property name. This survives process death and is trivial
     * to provide in unit tests.
     */
    private val artworkId: Int =
        checkNotNull(savedStateHandle[ArtworkDetailDestination.ARG_ARTWORK_ID])

    private val _state = MutableStateFlow(ArtworkDetailState())
    val state: StateFlow<ArtworkDetailState> = _state.asStateFlow()

    private val _effects = Channel<ArtworkDetailEffect>(Channel.BUFFERED)
    val effects: Flow<ArtworkDetailEffect> = _effects.receiveAsFlow()

    init {
        loadArtwork()
    }

    fun onIntent(intent: ArtworkDetailIntent) {
        when (intent) {
            ArtworkDetailIntent.Retry -> loadArtwork()
            ArtworkDetailIntent.BackClicked -> sendEffect(ArtworkDetailEffect.NavigateBack)
            ArtworkDetailIntent.OpenInBrowserClicked ->
                _state.value.artwork?.objectUrl?.let { sendEffect(ArtworkDetailEffect.OpenUrl(it)) }
        }
    }

    private fun loadArtwork() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getArtworkDetails(artworkId)
                .onSuccess { artwork -> _state.update { it.copy(artwork = artwork, isLoading = false) } }
                .onFailure { error ->
                    _state.update { it.copy(error = error.toErrorMessageRes(), isLoading = false) }
                }
        }
    }

    private fun sendEffect(effect: ArtworkDetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

package com.example.demo.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.domain.usecase.GetPhotosUseCase
import com.example.demo.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class PhotoListViewModel @Inject constructor(
    private val getPhotos: GetPhotosUseCase,
) : ViewModel() {

    // Private mutable, public read-only: only the ViewModel can change state.
    private val _state = MutableStateFlow(PhotoListState())
    val state: StateFlow<PhotoListState> = _state.asStateFlow()

    // Channel = each effect is consumed exactly once (unlike StateFlow, which replays).
    private val _effects = Channel<PhotoListEffect>(Channel.BUFFERED)
    val effects: Flow<PhotoListEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load(LoadType.Initial)
    }

    /** Single entry point for all user actions (unidirectional data flow). */
    fun onIntent(intent: PhotoListIntent) {
        when (intent) {
            PhotoListIntent.Retry -> load(LoadType.Initial)
            PhotoListIntent.Refresh -> load(LoadType.Refresh)
            PhotoListIntent.LoadMore -> load(LoadType.More)
            is PhotoListIntent.PhotoClicked -> sendEffect(PhotoListEffect.NavigateToDetail(intent.id))
        }
    }

    private fun load(type: LoadType) {
        val current = _state.value
        if (type == LoadType.More) {
            // Ignore duplicate scroll triggers and stop when the API has no more data.
            if (loadJob?.isActive == true || current.endReached || current.error != null) return
        }
        // A refresh/retry supersedes whatever is in flight.
        loadJob?.cancel()

        val page = if (type == LoadType.More) current.nextPage else FIRST_PAGE
        _state.update {
            it.copy(
                isLoading = type == LoadType.Initial,
                isRefreshing = type == LoadType.Refresh,
                isLoadingMore = type == LoadType.More,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            getPhotos(page)
                .onSuccess { newPhotos ->
                    _state.update {
                        val merged = if (page == FIRST_PAGE) newPhotos else it.photos + newPhotos
                        it.copy(
                            photos = merged.distinctBy { photo -> photo.id },
                            nextPage = page + 1,
                            endReached = newPhotos.size < GetPhotosUseCase.PAGE_SIZE,
                        ).stopLoading()
                    }
                }
                .onFailure { error ->
                    val message = error.toErrorMessageRes()
                    if (_state.value.photos.isEmpty()) {
                        // Nothing to show -> full-screen error with retry.
                        _state.update { it.copy(error = message).stopLoading() }
                    } else {
                        // Keep the content, just inform the user.
                        _state.update { it.stopLoading() }
                        sendEffect(PhotoListEffect.ShowSnackbar(message))
                    }
                }
        }
    }

    private fun PhotoListState.stopLoading() =
        copy(isLoading = false, isRefreshing = false, isLoadingMore = false)

    private fun sendEffect(effect: PhotoListEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private enum class LoadType { Initial, Refresh, More }

    private companion object {
        const val FIRST_PAGE = 1
    }
}

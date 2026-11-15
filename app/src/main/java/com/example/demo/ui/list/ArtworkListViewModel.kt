package com.example.demo.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.domain.usecase.GetArtworksUseCase
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
class ArtworkListViewModel @Inject constructor(
    private val getArtworks: GetArtworksUseCase,
) : ViewModel() {

    // Private mutable, public read-only: only the ViewModel can change state.
    private val _state = MutableStateFlow(ArtworkListState())
    val state: StateFlow<ArtworkListState> = _state.asStateFlow()

    // Channel = each effect is consumed exactly once (unlike StateFlow, which replays).
    private val _effects = Channel<ArtworkListEffect>(Channel.BUFFERED)
    val effects: Flow<ArtworkListEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load(LoadType.Initial)
    }

    /** Single entry point for all user actions (unidirectional data flow). */
    fun onIntent(intent: ArtworkListIntent) {
        when (intent) {
            ArtworkListIntent.Retry -> load(LoadType.Initial)
            ArtworkListIntent.Refresh -> load(LoadType.Refresh)
            ArtworkListIntent.LoadMore -> load(LoadType.More)
            ArtworkListIntent.RetryLoadMore -> {
                _state.update { it.copy(loadMoreError = null) }
                load(LoadType.More)
            }
            is ArtworkListIntent.ArtworkClicked ->
                sendEffect(ArtworkListEffect.NavigateToDetail(intent.id))
        }
    }

    private fun load(type: LoadType) {
        val current = _state.value
        if (type == LoadType.More) {
            // Ignore duplicate scroll triggers, stop at the end of the data, and never
            // auto-retry a failed page (that would hammer the API in a loop).
            if (loadJob?.isActive == true || current.endReached || current.loadMoreError != null) return
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
                loadMoreError = null,
            )
        }

        loadJob = viewModelScope.launch {
            getArtworks(page)
                .onSuccess { result ->
                    _state.update {
                        val merged =
                            if (page == FIRST_PAGE) result.artworks else it.artworks + result.artworks
                        it.copy(
                            artworks = merged.distinctBy { artwork -> artwork.id },
                            nextPage = page + 1,
                            endReached = !result.hasMore,
                        ).stopLoading()
                    }
                }
                .onFailure { error ->
                    val message = error.toErrorMessageRes()
                    if (_state.value.artworks.isEmpty()) {
                        // Nothing to show -> full-screen error with retry.
                        _state.update { it.copy(error = message).stopLoading() }
                    } else if (type == LoadType.More) {
                        // Next page failed -> inline retry footer at the end of the grid.
                        _state.update { it.copy(loadMoreError = message).stopLoading() }
                    } else {
                        // Keep the content, just inform the user.
                        _state.update { it.stopLoading() }
                        sendEffect(ArtworkListEffect.ShowSnackbar(message))
                    }
                }
        }
    }

    private fun ArtworkListState.stopLoading() =
        copy(isLoading = false, isRefreshing = false, isLoadingMore = false)

    private fun sendEffect(effect: ArtworkListEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private enum class LoadType { Initial, Refresh, More }

    private companion object {
        const val FIRST_PAGE = 0
    }
}

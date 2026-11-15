package com.example.demo.ui.list

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.demo.domain.model.Artwork

/**
 * MVI contract for the artwork list screen.
 *
 * State  - everything the UI needs to render, as ONE immutable object (single source of truth).
 * Intent - everything the user can do; the only way the UI talks to the ViewModel.
 * Effect - one-off events (navigation, snackbar) that must not be re-delivered on rotation.
 */
@Immutable
data class ArtworkListState(
    val artworks: List<Artwork> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    @StringRes val error: Int? = null,
    /** Set when loading the NEXT page failed; paging pauses until the user retries. */
    @StringRes val loadMoreError: Int? = null,
    val nextPage: Int = 0,
    val endReached: Boolean = false,
)

sealed interface ArtworkListIntent {
    data object Retry : ArtworkListIntent
    data object Refresh : ArtworkListIntent
    data object LoadMore : ArtworkListIntent
    data object RetryLoadMore : ArtworkListIntent
    data class ArtworkClicked(val id: Int) : ArtworkListIntent
}

sealed interface ArtworkListEffect {
    data class NavigateToDetail(val id: Int) : ArtworkListEffect
    data class ShowSnackbar(@StringRes val message: Int) : ArtworkListEffect
}

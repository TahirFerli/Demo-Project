package com.example.demo.ui.list

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.demo.domain.model.Photo

/**
 * MVI contract for the photo list screen.
 *
 * State  - everything the UI needs to render, as ONE immutable object (single source of truth).
 * Intent - everything the user can do; the only way the UI talks to the ViewModel.
 * Effect - one-off events (navigation, snackbar) that must not be re-delivered on rotation.
 */
@Immutable
data class PhotoListState(
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    @StringRes val error: Int? = null,
    val nextPage: Int = 1,
    val endReached: Boolean = false,
)

sealed interface PhotoListIntent {
    data object Retry : PhotoListIntent
    data object Refresh : PhotoListIntent
    data object LoadMore : PhotoListIntent
    data class PhotoClicked(val id: String) : PhotoListIntent
}

sealed interface PhotoListEffect {
    data class NavigateToDetail(val id: String) : PhotoListEffect
    data class ShowSnackbar(@StringRes val message: Int) : PhotoListEffect
}

package com.example.demo.ui.detail

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.demo.domain.model.Artwork

@Immutable
data class ArtworkDetailState(
    val artwork: Artwork? = null,
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null,
)

sealed interface ArtworkDetailIntent {
    data object Retry : ArtworkDetailIntent
    data object BackClicked : ArtworkDetailIntent
    data object OpenInBrowserClicked : ArtworkDetailIntent
}

sealed interface ArtworkDetailEffect {
    data object NavigateBack : ArtworkDetailEffect
    data class OpenUrl(val url: String) : ArtworkDetailEffect
}

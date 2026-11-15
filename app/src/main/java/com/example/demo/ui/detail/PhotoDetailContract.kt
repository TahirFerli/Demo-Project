package com.example.demo.ui.detail

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.demo.domain.model.Photo

@Immutable
data class PhotoDetailState(
    val photo: Photo? = null,
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null,
)

sealed interface PhotoDetailIntent {
    data object Retry : PhotoDetailIntent
    data object BackClicked : PhotoDetailIntent
    data object OpenSourceClicked : PhotoDetailIntent
}

sealed interface PhotoDetailEffect {
    data object NavigateBack : PhotoDetailEffect
    data class OpenUrl(val url: String) : PhotoDetailEffect
}

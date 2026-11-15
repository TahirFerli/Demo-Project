package com.example.demo.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.example.demo.ui.theme.DemoProjectTheme

/**
 * Wraps @Preview content in the app theme and makes Coil's AsyncImage draw a solid
 * placeholder. Previews run in Android Studio's layoutlib, which has no network,
 * so without this every image slot is blank.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun PreviewContainer(content: @Composable () -> Unit) {
    val previewImages = AsyncImagePreviewHandler { ColorImage(PreviewImageColor.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewImages) {
        DemoProjectTheme(dynamicColor = false) { content() }
    }
}

private val PreviewImageColor = Color(0xFF8C7B5E)

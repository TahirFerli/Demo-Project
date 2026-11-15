package com.example.demo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.demo.R
import com.example.demo.domain.model.Photo
import com.example.demo.ui.common.FullScreenError
import com.example.demo.ui.common.FullScreenLoading
import com.example.demo.ui.common.ObserveAsEvents
import com.example.demo.ui.theme.DemoProjectTheme

@Composable
fun PhotoDetailRoute(
    onNavigateBack: () -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            PhotoDetailEffect.NavigateBack -> onNavigateBack()
            is PhotoDetailEffect.OpenUrl -> uriHandler.openUri(effect.url)
        }
    }

    PhotoDetailScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    state: PhotoDetailState,
    onIntent: (PhotoDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.photo?.author ?: stringResource(R.string.photo_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(PhotoDetailIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            state.photo != null -> PhotoDetailContent(
                photo = state.photo,
                onOpenSource = { onIntent(PhotoDetailIntent.OpenSourceClicked) },
                modifier = contentModifier,
            )
            state.error != null -> FullScreenError(
                message = state.error,
                onRetry = { onIntent(PhotoDetailIntent.Retry) },
                modifier = contentModifier,
            )
            else -> FullScreenLoading(contentModifier)
        }
    }
}

@Composable
private fun PhotoDetailContent(
    photo: Photo,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = photo.largeImageUrl,
            contentDescription = stringResource(R.string.photo_by, photo.author),
            contentScale = ContentScale.Fit,
            // Reserve the final size up-front so the layout doesn't jump when the image loads.
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(photo.aspectRatio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        DetailRow(stringResource(R.string.detail_author), photo.author)
        HorizontalDivider()
        DetailRow(
            stringResource(R.string.detail_resolution),
            stringResource(R.string.detail_resolution_value, photo.width, photo.height),
        )
        HorizontalDivider()
        DetailRow(stringResource(R.string.detail_id), photo.id)
        Button(
            onClick = onOpenSource,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Text(stringResource(R.string.detail_open_source), Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = { Text(value) },
    )
}

@Preview(showBackground = true)
@Composable
private fun PhotoDetailScreenPreview() {
    DemoProjectTheme {
        PhotoDetailScreen(
            state = PhotoDetailState(
                photo = Photo(
                    id = "10", author = "Paul Jarvis", width = 2500, height = 1667,
                    sourceUrl = "", thumbnailUrl = "", largeImageUrl = "", fullImageUrl = "",
                ),
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}

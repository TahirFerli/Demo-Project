package com.example.demo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.example.demo.domain.model.Artwork
import com.example.demo.ui.common.FullScreenError
import com.example.demo.ui.common.FullScreenLoading
import com.example.demo.ui.common.ObserveAsEvents
import com.example.demo.ui.list.previewArtwork
import com.example.demo.ui.theme.DemoProjectTheme

@Composable
fun ArtworkDetailRoute(
    onNavigateBack: () -> Unit,
    viewModel: ArtworkDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            ArtworkDetailEffect.NavigateBack -> onNavigateBack()
            is ArtworkDetailEffect.OpenUrl -> uriHandler.openUri(effect.url)
        }
    }

    ArtworkDetailScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkDetailScreen(
    state: ArtworkDetailState,
    onIntent: (ArtworkDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.artwork?.title ?: stringResource(R.string.artwork_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ArtworkDetailIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            state.artwork != null -> ArtworkDetailContent(
                artwork = state.artwork,
                onOpenInBrowser = { onIntent(ArtworkDetailIntent.OpenInBrowserClicked) },
                modifier = contentModifier,
            )
            state.error != null -> FullScreenError(
                message = state.error,
                onRetry = { onIntent(ArtworkDetailIntent.Retry) },
                modifier = contentModifier,
            )
            else -> FullScreenLoading(contentModifier)
        }
    }
}

@Composable
private fun ArtworkDetailContent(
    artwork: Artwork,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = artwork.title ?: stringResource(R.string.untitled)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = artwork.imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            // Aspect ratio isn't known up-front, so bound the height to limit layout jumps.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 520.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = listOfNotNull(artwork.artist, artwork.artistBio).joinToString(" · ")
                    .ifEmpty { stringResource(R.string.unknown_artist) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Only show rows that have data.
        listOf(
            R.string.detail_date to artwork.date,
            R.string.detail_medium to artwork.medium,
            R.string.detail_dimensions to artwork.dimensions,
            R.string.detail_culture to artwork.culture,
            R.string.detail_department to artwork.department,
            R.string.detail_credit_line to artwork.creditLine,
        ).forEach { (label, value) ->
            if (value != null) {
                HorizontalDivider()
                DetailRow(stringResource(label), value)
            }
        }
        if (artwork.objectUrl != null) {
            Button(
                onClick = onOpenInBrowser,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text(stringResource(R.string.detail_open_in_browser), Modifier.padding(start = 8.dp))
            }
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
private fun ArtworkDetailScreenPreview() {
    DemoProjectTheme {
        ArtworkDetailScreen(
            state = ArtworkDetailState(artwork = previewArtwork(436535), isLoading = false),
            onIntent = {},
        )
    }
}

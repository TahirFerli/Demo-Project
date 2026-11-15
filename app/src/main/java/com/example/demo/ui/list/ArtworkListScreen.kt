package com.example.demo.ui.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demo.R
import com.example.demo.domain.model.Artwork
import com.example.demo.ui.common.FullScreenError
import com.example.demo.ui.common.FullScreenLoading
import com.example.demo.ui.common.ObserveAsEvents
import com.example.demo.ui.common.TestTags
import com.example.demo.ui.theme.DemoProjectTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LOAD_MORE_THRESHOLD = 6

/**
 * Stateful "route" composable: wires the ViewModel to the stateless screen.
 * This is the ONLY composable that knows about the ViewModel.
 */
@Composable
fun ArtworkListRoute(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ArtworkListViewModel = hiltViewModel(),
) {
    // Lifecycle-aware: stops collecting when the app is in the background.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is ArtworkListEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
            is ArtworkListEffect.ShowSnackbar ->
                snackbarHostState.showSnackbar(context.getString(effect.message))
        }
    }

    ArtworkListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

/** Stateless screen: state in, intents out. Easy to preview and UI-test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkListScreen(
    state: ArtworkListState,
    onIntent: (ArtworkListIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.artwork_list_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            state.isLoading -> FullScreenLoading(contentModifier)
            state.error != null -> FullScreenError(
                message = state.error,
                onRetry = { onIntent(ArtworkListIntent.Retry) },
                modifier = contentModifier,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(ArtworkListIntent.Refresh) },
                modifier = contentModifier.fillMaxSize(),
            ) {
                ArtworkGrid(
                    artworks = state.artworks,
                    isLoadingMore = state.isLoadingMore,
                    loadMoreError = state.loadMoreError,
                    onArtworkClick = { onIntent(ArtworkListIntent.ArtworkClicked(it)) },
                    onLoadMore = { onIntent(ArtworkListIntent.LoadMore) },
                    onRetryLoadMore = { onIntent(ArtworkListIntent.RetryLoadMore) },
                )
            }
        }
    }
}

@Composable
private fun ArtworkGrid(
    artworks: List<Artwork>,
    isLoadingMore: Boolean,
    @StringRes loadMoreError: Int?,
    onArtworkClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    // Keep the latest lambda without restarting the effect below.
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    // snapshotFlow turns Compose state into a Flow, so we can use Flow operators on scroll position.
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearEnd = info.totalItemsCount > 0 &&
                lastVisible >= info.totalItemsCount - LOAD_MORE_THRESHOLD
            // Include the item count so a page that adds few/no items re-triggers loading.
            nearEnd to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { currentOnLoadMore() }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize().testTag(TestTags.ARTWORK_GRID),
    ) {
        // Stable keys: items keep their state/animations when the list changes.
        items(items = artworks, key = { it.id }, contentType = { "artwork" }) { artwork ->
            ArtworkItem(artwork = artwork, onClick = { onArtworkClick(artwork.id) })
        }
        if (isLoadingMore || loadMoreError != null) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    if (loadMoreError != null) {
                        LoadMoreError(message = loadMoreError, onRetry = onRetryLoadMore)
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadMoreError(@StringRes message: Int, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

internal fun previewArtwork(id: Int) = Artwork(
    id = id, title = "Wheat Field with Cypresses", artist = "Vincent van Gogh",
    artistBio = "Dutch, 1853–1890", date = "1889", medium = "Oil on canvas",
    dimensions = "28 7/8 × 36 3/4 in. (73.2 × 93.4 cm)", department = "European Paintings",
    culture = null, creditLine = "Purchase, The Annenberg Foundation Gift, 1993",
    imageUrl = "", objectUrl = "https://www.metmuseum.org/art/collection/search/436535",
)

@Preview(showBackground = true)
@Composable
private fun ArtworkListScreenPreview() {
    DemoProjectTheme {
        ArtworkListScreen(
            state = ArtworkListState(artworks = List(6) { previewArtwork(it) }),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArtworkListScreenErrorPreview() {
    DemoProjectTheme {
        ArtworkListScreen(state = ArtworkListState(error = R.string.error_network), onIntent = {})
    }
}

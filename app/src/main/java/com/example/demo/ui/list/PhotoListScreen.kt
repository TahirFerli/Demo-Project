package com.example.demo.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demo.R
import com.example.demo.domain.model.Photo
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
fun PhotoListRoute(
    onNavigateToDetail: (String) -> Unit,
    viewModel: PhotoListViewModel = hiltViewModel(),
) {
    // Lifecycle-aware: stops collecting when the app is in the background.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is PhotoListEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
            is PhotoListEffect.ShowSnackbar ->
                snackbarHostState.showSnackbar(context.getString(effect.message))
        }
    }

    PhotoListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

/** Stateless screen: state in, intents out. Easy to preview and UI-test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(
    state: PhotoListState,
    onIntent: (PhotoListIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.photo_list_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            state.isLoading -> FullScreenLoading(contentModifier)
            state.error != null -> FullScreenError(
                message = state.error,
                onRetry = { onIntent(PhotoListIntent.Retry) },
                modifier = contentModifier,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(PhotoListIntent.Refresh) },
                modifier = contentModifier.fillMaxSize(),
            ) {
                PhotoGrid(
                    photos = state.photos,
                    isLoadingMore = state.isLoadingMore,
                    onPhotoClick = { onIntent(PhotoListIntent.PhotoClicked(it)) },
                    onLoadMore = { onIntent(PhotoListIntent.LoadMore) },
                )
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    isLoadingMore: Boolean,
    onPhotoClick: (String) -> Unit,
    onLoadMore: () -> Unit,
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
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadMore() }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize().testTag(TestTags.PHOTO_GRID),
    ) {
        // Stable keys: items keep their state/animations when the list changes.
        items(items = photos, key = { it.id }, contentType = { "photo" }) { photo ->
            PhotoItem(photo = photo, onClick = { onPhotoClick(photo.id) })
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "loader") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private val previewPhotos = List(6) {
    Photo(
        id = "$it", author = "Author $it", width = 400, height = 300,
        sourceUrl = "", thumbnailUrl = "", largeImageUrl = "", fullImageUrl = "",
    )
}

@Preview(showBackground = true)
@Composable
private fun PhotoListScreenPreview() {
    DemoProjectTheme {
        PhotoListScreen(state = PhotoListState(photos = previewPhotos), onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoListScreenErrorPreview() {
    DemoProjectTheme {
        PhotoListScreen(state = PhotoListState(error = R.string.error_network), onIntent = {})
    }
}

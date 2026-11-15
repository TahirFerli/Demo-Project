package com.example.demo.ui.list

import app.cash.turbine.test
import com.example.demo.R
import com.example.demo.domain.model.ArtworkPage
import com.example.demo.domain.model.DataError
import com.example.demo.domain.usecase.GetArtworksUseCase
import com.example.demo.domain.usecase.GetArtworksUseCase.Companion.PAGE_SIZE
import com.example.demo.util.FakeArtworkRepository
import com.example.demo.util.MainDispatcherRule
import com.example.demo.util.testArtworks
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArtworkListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeArtworkRepository()

    private fun createViewModel() = ArtworkListViewModel(GetArtworksUseCase(repository))

    @Test
    fun `initial load shows loading then artworks`() = runTest {
        val viewModel = createViewModel()

        // StandardTestDispatcher: the coroutine hasn't run yet, so we can observe the loading state.
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(PAGE_SIZE, state.artworks.size)
        assertEquals(1, state.nextPage)
        assertNull(state.error)
    }

    @Test
    fun `initial load failure shows full screen error`() = runTest {
        repository.artworksResult = { _, _ -> Result.failure(DataError.Network()) }

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(R.string.error_network, state.error)
        assertTrue(state.artworks.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `retry after failure loads artworks`() = runTest {
        repository.artworksResult = { _, _ -> Result.failure(DataError.Network()) }
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.artworksResult = { page, size ->
            Result.success(ArtworkPage(testArtworks(page, size), hasMore = true))
        }
        viewModel.onIntent(ArtworkListIntent.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(PAGE_SIZE, viewModel.state.value.artworks.size)
    }

    @Test
    fun `load more appends next page`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(0, 1), repository.requestedPages)
        assertEquals(PAGE_SIZE * 2, viewModel.state.value.artworks.size)
        assertEquals(2, viewModel.state.value.nextPage)
    }

    @Test
    fun `duplicate load more while loading triggers a single request`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ArtworkListIntent.LoadMore)
        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(0, 1), repository.requestedPages)
    }

    @Test
    fun `hasMore false marks end reached and stops paging`() = runTest {
        repository.artworksResult = { page, size ->
            Result.success(ArtworkPage(testArtworks(page, size), hasMore = false))
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.endReached)
        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(0), repository.requestedPages)
    }

    @Test
    fun `load more failure shows inline error and pauses paging`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.artworksResult = { _, _ -> Result.failure(DataError.RateLimited()) }

        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()
        // Scroll keeps firing LoadMore - it must NOT retry automatically.
        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(R.string.error_rate_limited, state.loadMoreError)
        assertEquals(PAGE_SIZE, state.artworks.size)
        assertNull(state.error)
        assertEquals(listOf(0, 1), repository.requestedPages)
    }

    @Test
    fun `retry load more clears error and loads next page`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.artworksResult = { _, _ -> Result.failure(DataError.Network()) }
        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()

        repository.artworksResult = { page, size ->
            Result.success(ArtworkPage(testArtworks(page, size), hasMore = true))
        }
        viewModel.onIntent(ArtworkListIntent.RetryLoadMore)
        advanceUntilIdle()

        assertNull(viewModel.state.value.loadMoreError)
        assertEquals(PAGE_SIZE * 2, viewModel.state.value.artworks.size)
    }

    @Test
    fun `refresh failure with content shows snackbar`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.artworksResult = { _, _ -> Result.failure(DataError.Server(code = 500)) }

        viewModel.effects.test {
            viewModel.onIntent(ArtworkListIntent.Refresh)
            assertEquals(ArtworkListEffect.ShowSnackbar(R.string.error_server), awaitItem())
        }
        assertEquals(PAGE_SIZE, viewModel.state.value.artworks.size)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `refresh reloads first page`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(ArtworkListIntent.LoadMore)
        advanceUntilIdle()

        viewModel.onIntent(ArtworkListIntent.Refresh)
        assertTrue(viewModel.state.value.isRefreshing)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(PAGE_SIZE, viewModel.state.value.artworks.size)
        assertEquals(listOf(0, 1, 0), repository.requestedPages)
    }

    @Test
    fun `clicking an artwork emits navigation effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(ArtworkListIntent.ArtworkClicked(42))
            assertEquals(ArtworkListEffect.NavigateToDetail(42), awaitItem())
        }
    }
}

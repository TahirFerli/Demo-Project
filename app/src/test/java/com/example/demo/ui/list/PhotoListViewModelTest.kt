package com.example.demo.ui.list

import app.cash.turbine.test
import com.example.demo.R
import com.example.demo.domain.model.DataError
import com.example.demo.domain.usecase.GetPhotosUseCase
import com.example.demo.domain.usecase.GetPhotosUseCase.Companion.PAGE_SIZE
import com.example.demo.util.FakePhotoRepository
import com.example.demo.util.MainDispatcherRule
import com.example.demo.util.testPhotos
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PhotoListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePhotoRepository

    @Before
    fun setUp() {
        repository = FakePhotoRepository()
    }

    private fun createViewModel() = PhotoListViewModel(GetPhotosUseCase(repository))

    @Test
    fun `initial load shows loading then photos`() = runTest {
        val viewModel = createViewModel()

        // StandardTestDispatcher: the coroutine hasn't run yet, so we can observe the loading state.
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(PAGE_SIZE, state.photos.size)
        assertEquals(2, state.nextPage)
        assertNull(state.error)
    }

    @Test
    fun `initial load failure shows full screen error`() = runTest {
        repository.photosResult = { _, _ -> Result.failure(DataError.Network()) }

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(R.string.error_network, state.error)
        assertTrue(state.photos.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `retry after failure loads photos`() = runTest {
        repository.photosResult = { _, _ -> Result.failure(DataError.Network()) }
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.photosResult = { page, size -> Result.success(testPhotos(page, size)) }
        viewModel.onIntent(PhotoListIntent.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(PAGE_SIZE, viewModel.state.value.photos.size)
    }

    @Test
    fun `load more appends next page`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PhotoListIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.requestedPages)
        assertEquals(PAGE_SIZE * 2, viewModel.state.value.photos.size)
        assertEquals(3, viewModel.state.value.nextPage)
    }

    @Test
    fun `duplicate load more while loading triggers a single request`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PhotoListIntent.LoadMore)
        viewModel.onIntent(PhotoListIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun `short page marks end reached and stops paging`() = runTest {
        repository.photosResult = { page, _ -> Result.success(testPhotos(page, pageSize = 5)) }
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.endReached)
        viewModel.onIntent(PhotoListIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(1), repository.requestedPages)
    }

    @Test
    fun `load more failure keeps content and shows snackbar`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        repository.photosResult = { _, _ -> Result.failure(DataError.Server(code = 500)) }

        viewModel.effects.test {
            viewModel.onIntent(PhotoListIntent.LoadMore)
            assertEquals(PhotoListEffect.ShowSnackbar(R.string.error_server), awaitItem())
        }
        assertEquals(PAGE_SIZE, viewModel.state.value.photos.size)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `refresh reloads first page`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(PhotoListIntent.LoadMore)
        advanceUntilIdle()

        viewModel.onIntent(PhotoListIntent.Refresh)
        assertTrue(viewModel.state.value.isRefreshing)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(PAGE_SIZE, viewModel.state.value.photos.size)
        assertEquals(listOf(1, 2, 1), repository.requestedPages)
    }

    @Test
    fun `clicking a photo emits navigation effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(PhotoListIntent.PhotoClicked("42"))
            assertEquals(PhotoListEffect.NavigateToDetail("42"), awaitItem())
        }
    }
}

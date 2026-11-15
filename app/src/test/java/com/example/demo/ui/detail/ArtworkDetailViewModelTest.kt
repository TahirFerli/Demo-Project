package com.example.demo.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.demo.R
import com.example.demo.domain.model.DataError
import com.example.demo.domain.usecase.GetArtworkDetailsUseCase
import com.example.demo.ui.navigation.ArtworkDetailDestination
import com.example.demo.util.FakeArtworkRepository
import com.example.demo.util.MainDispatcherRule
import com.example.demo.util.testArtwork
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ArtworkDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeArtworkRepository()

    private fun createViewModel(id: Int = 7) = ArtworkDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ArtworkDetailDestination.ARG_ARTWORK_ID to id)),
        getArtworkDetails = GetArtworkDetailsUseCase(repository),
    )

    @Test
    fun `loads artwork for id from navigation arguments`() = runTest {
        val viewModel = createViewModel(id = 7)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(testArtwork(7), state.artwork)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `failure shows error and retry recovers`() = runTest {
        repository.artworkResult = { Result.failure(DataError.NotFound()) }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(R.string.error_not_found, viewModel.state.value.error)

        repository.artworkResult = { id -> Result.success(testArtwork(id)) }
        viewModel.onIntent(ArtworkDetailIntent.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(7, viewModel.state.value.artwork?.id)
    }

    @Test
    fun `open in browser emits url effect`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(ArtworkDetailIntent.OpenInBrowserClicked)
            assertEquals(
                ArtworkDetailEffect.OpenUrl("https://www.metmuseum.org/art/collection/search/7"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `back click emits navigate back effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(ArtworkDetailIntent.BackClicked)
            assertEquals(ArtworkDetailEffect.NavigateBack, awaitItem())
        }
    }
}

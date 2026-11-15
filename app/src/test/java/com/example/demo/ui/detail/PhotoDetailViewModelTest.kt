package com.example.demo.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.demo.R
import com.example.demo.domain.model.DataError
import com.example.demo.domain.usecase.GetPhotoDetailsUseCase
import com.example.demo.ui.navigation.PhotoDetailDestination
import com.example.demo.util.FakePhotoRepository
import com.example.demo.util.MainDispatcherRule
import com.example.demo.util.testPhoto
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PhotoDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePhotoRepository()

    private fun createViewModel(id: String = "7") = PhotoDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PhotoDetailDestination.ARG_PHOTO_ID to id)),
        getPhotoDetails = GetPhotoDetailsUseCase(repository),
    )

    @Test
    fun `loads photo for id from navigation arguments`() = runTest {
        val viewModel = createViewModel(id = "7")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(testPhoto("7"), state.photo)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `failure shows error and retry recovers`() = runTest {
        repository.photoResult = { Result.failure(DataError.Network()) }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(R.string.error_network, viewModel.state.value.error)

        repository.photoResult = { id -> Result.success(testPhoto(id)) }
        viewModel.onIntent(PhotoDetailIntent.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals("7", viewModel.state.value.photo?.id)
    }

    @Test
    fun `open source emits url effect`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(PhotoDetailIntent.OpenSourceClicked)
            assertEquals(PhotoDetailEffect.OpenUrl("https://unsplash.com/photos/7"), awaitItem())
        }
    }

    @Test
    fun `back click emits navigate back effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(PhotoDetailIntent.BackClicked)
            assertEquals(PhotoDetailEffect.NavigateBack, awaitItem())
        }
    }
}

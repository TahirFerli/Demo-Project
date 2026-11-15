package com.example.demo.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.demo.R
import com.example.demo.ui.common.TestTags
import com.example.demo.ui.theme.DemoProjectTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Because ArtworkListScreen is stateless (state hoisting), we can test every UI state
 * by passing a state object - no ViewModel, Hilt or network needed.
 */
@RunWith(AndroidJUnit4::class)
class ArtworkListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ArtworkListIntent>()

    private fun setScreen(state: ArtworkListState) {
        composeRule.setContent {
            DemoProjectTheme {
                ArtworkListScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    @Test
    fun loadingState_showsProgress() {
        setScreen(ArtworkListState(isLoading = true))

        composeRule.onNodeWithTag(TestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_sendsRetryIntent() {
        setScreen(ArtworkListState(error = R.string.error_network))

        composeRule.onNodeWithTag(TestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(listOf(ArtworkListIntent.Retry), intents)
    }

    @Test
    fun contentState_artworkClick_sendsArtworkClickedIntent() {
        val artworks = listOf(
            previewArtwork(1).copy(title = "Artwork one"),
            previewArtwork(2).copy(title = "Artwork two"),
        )
        setScreen(ArtworkListState(artworks = artworks))

        composeRule.onNodeWithText("Artwork two").assertIsDisplayed().performClick()

        assertEquals(ArtworkListIntent.ArtworkClicked(2), intents.last())
    }

    @Test
    fun missingTitleAndArtist_showFallbackText() {
        setScreen(ArtworkListState(artworks = listOf(previewArtwork(1).copy(title = null, artist = null))))

        composeRule.onNodeWithText("Untitled").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertIsDisplayed()
    }

    @Test
    fun loadMoreError_retryClick_sendsRetryLoadMoreIntent() {
        setScreen(
            ArtworkListState(
                artworks = listOf(previewArtwork(1)),
                loadMoreError = R.string.error_rate_limited,
            ),
        )

        composeRule.onNodeWithText("Retry").performScrollTo().performClick()

        assertEquals(ArtworkListIntent.RetryLoadMore, intents.last())
    }
}

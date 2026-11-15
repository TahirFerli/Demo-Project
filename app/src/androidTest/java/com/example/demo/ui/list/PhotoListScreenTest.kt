package com.example.demo.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.demo.R
import com.example.demo.domain.model.Photo
import com.example.demo.ui.common.TestTags
import com.example.demo.ui.theme.DemoProjectTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Because PhotoListScreen is stateless (state hoisting), we can test every UI state
 * by passing a state object - no ViewModel, Hilt or network needed.
 */
@RunWith(AndroidJUnit4::class)
class PhotoListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<PhotoListIntent>()

    private fun setScreen(state: PhotoListState) {
        composeRule.setContent {
            DemoProjectTheme {
                PhotoListScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    @Test
    fun loadingState_showsProgress() {
        setScreen(PhotoListState(isLoading = true))

        composeRule.onNodeWithTag(TestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_sendsRetryIntent() {
        setScreen(PhotoListState(error = R.string.error_network))

        composeRule.onNodeWithTag(TestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(listOf(PhotoListIntent.Retry), intents)
    }

    @Test
    fun contentState_photoClick_sendsPhotoClickedIntent() {
        setScreen(PhotoListState(photos = listOf(photo("1"), photo("2"))))

        composeRule.onNodeWithText("Author 2").assertIsDisplayed().performClick()

        assertEquals(PhotoListIntent.PhotoClicked("2"), intents.last())
    }

    private fun photo(id: String) = Photo(
        id = id, author = "Author $id", width = 400, height = 300,
        sourceUrl = "", thumbnailUrl = "", largeImageUrl = "", fullImageUrl = "",
    )
}

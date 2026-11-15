package com.example.demo.domain.model

/**
 * Domain model. Free of any framework/serialization annotations so the
 * UI never depends on the shape of the network response.
 */
data class Photo(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    /** Original page on Unsplash. */
    val sourceUrl: String,
    /** Small, resized image for list cells (saves bandwidth & memory). */
    val thumbnailUrl: String,
    /** Screen-sized image for the detail page. */
    val largeImageUrl: String,
    /** Full resolution image. */
    val fullImageUrl: String,
) {
    val aspectRatio: Float get() = if (height == 0) 1f else width.toFloat() / height
}

package com.example.demo.data.mapper

import com.example.demo.data.remote.PicsumApi
import com.example.demo.data.remote.dto.PhotoDto
import com.example.demo.domain.model.Photo

private const val THUMBNAIL_WIDTH = 400
private const val LARGE_WIDTH = 1080

/**
 * Picsum can resize on the server: /id/{id}/{width}/{height}.
 * Requesting a small thumbnail for the grid avoids decoding 5000px images.
 */
fun PhotoDto.toDomain(): Photo {
    fun resizedUrl(targetWidth: Int): String {
        val targetHeight = if (width == 0) targetWidth else targetWidth * height / width
        return "${PicsumApi.BASE_URL}id/$id/$targetWidth/$targetHeight"
    }
    return Photo(
        id = id,
        author = author,
        width = width,
        height = height,
        sourceUrl = url,
        thumbnailUrl = resizedUrl(THUMBNAIL_WIDTH),
        largeImageUrl = resizedUrl(LARGE_WIDTH),
        fullImageUrl = downloadUrl,
    )
}

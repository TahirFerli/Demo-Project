package com.example.demo.data.mapper

import com.example.demo.data.remote.PicsumApi
import com.example.demo.data.remote.dto.PhotoDto
import com.example.demo.domain.model.Photo

private const val THUMBNAIL_WIDTH = 400

/**
 * Picsum can resize on the server: /id/{id}/{width}/{height}.
 * Requesting a small thumbnail for the grid avoids decoding 5000px images.
 */
fun PhotoDto.toDomain(): Photo {
    val thumbHeight = if (width == 0) THUMBNAIL_WIDTH else THUMBNAIL_WIDTH * height / width
    return Photo(
        id = id,
        author = author,
        width = width,
        height = height,
        sourceUrl = url,
        thumbnailUrl = "${PicsumApi.BASE_URL}id/$id/$THUMBNAIL_WIDTH/$thumbHeight",
        fullImageUrl = downloadUrl,
    )
}

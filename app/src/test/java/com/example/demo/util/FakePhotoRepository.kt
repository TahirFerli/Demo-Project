package com.example.demo.util

import com.example.demo.domain.model.Photo
import com.example.demo.domain.repository.PhotoRepository

/** Hand-written fake: simpler and more readable than mocks for a small interface. */
class FakePhotoRepository : PhotoRepository {

    var photosResult: (page: Int, pageSize: Int) -> Result<List<Photo>> =
        { page, pageSize -> Result.success(testPhotos(page, pageSize)) }
    var photoResult: (id: String) -> Result<Photo> = { id -> Result.success(testPhoto(id)) }

    val requestedPages = mutableListOf<Int>()

    override suspend fun getPhotos(page: Int, pageSize: Int): Result<List<Photo>> {
        requestedPages += page
        return photosResult(page, pageSize)
    }

    override suspend fun getPhoto(id: String): Result<Photo> = photoResult(id)
}

fun testPhoto(id: String) = Photo(
    id = id,
    author = "Author $id",
    width = 400,
    height = 300,
    sourceUrl = "https://unsplash.com/photos/$id",
    thumbnailUrl = "thumb/$id",
    largeImageUrl = "large/$id",
    fullImageUrl = "full/$id",
)

fun testPhotos(page: Int, pageSize: Int) =
    List(pageSize) { index -> testPhoto(id = "${(page - 1) * pageSize + index}") }

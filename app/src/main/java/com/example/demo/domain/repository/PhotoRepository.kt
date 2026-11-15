package com.example.demo.domain.repository

import com.example.demo.domain.model.Photo

/**
 * Abstraction owned by the domain layer. The data layer provides the
 * implementation (dependency inversion), which also lets tests swap in a fake.
 */
interface PhotoRepository {
    suspend fun getPhotos(page: Int, pageSize: Int): Result<List<Photo>>
    suspend fun getPhoto(id: String): Result<Photo>
}

package com.example.demo.domain.usecase

import com.example.demo.domain.model.Photo
import com.example.demo.domain.repository.PhotoRepository
import javax.inject.Inject

/**
 * Single business action: load one page of photos.
 * Keeps paging rules (page size) out of the ViewModel.
 */
class GetPhotosUseCase @Inject constructor(
    private val repository: PhotoRepository,
) {
    suspend operator fun invoke(page: Int): Result<List<Photo>> =
        repository.getPhotos(page = page, pageSize = PAGE_SIZE)

    companion object {
        const val PAGE_SIZE = 30
    }
}

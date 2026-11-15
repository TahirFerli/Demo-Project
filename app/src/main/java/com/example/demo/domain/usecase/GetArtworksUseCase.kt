package com.example.demo.domain.usecase

import com.example.demo.domain.model.ArtworkPage
import com.example.demo.domain.repository.ArtworkRepository
import javax.inject.Inject

/**
 * Single business action: load one page of artworks.
 * Keeps paging rules (page size) out of the ViewModel.
 */
class GetArtworksUseCase @Inject constructor(
    private val repository: ArtworkRepository,
) {
    suspend operator fun invoke(page: Int): Result<ArtworkPage> =
        repository.getArtworks(page = page, pageSize = PAGE_SIZE)

    companion object {
        /** Each item needs its own request, so pages are kept small. */
        const val PAGE_SIZE = 20
    }
}

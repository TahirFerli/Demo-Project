package com.example.demo.domain.repository

import com.example.demo.domain.model.Artwork
import com.example.demo.domain.model.ArtworkPage

/**
 * Abstraction owned by the domain layer. The data layer provides the
 * implementation (dependency inversion), which also lets tests swap in a fake.
 */
interface ArtworkRepository {
    /** [page] is zero-based. */
    suspend fun getArtworks(page: Int, pageSize: Int): Result<ArtworkPage>
    suspend fun getArtwork(id: Int): Result<Artwork>
}

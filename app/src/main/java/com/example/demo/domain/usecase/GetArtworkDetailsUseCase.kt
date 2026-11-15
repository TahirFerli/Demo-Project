package com.example.demo.domain.usecase

import com.example.demo.domain.model.Artwork
import com.example.demo.domain.repository.ArtworkRepository
import javax.inject.Inject

class GetArtworkDetailsUseCase @Inject constructor(
    private val repository: ArtworkRepository,
) {
    suspend operator fun invoke(id: Int): Result<Artwork> = repository.getArtwork(id)
}

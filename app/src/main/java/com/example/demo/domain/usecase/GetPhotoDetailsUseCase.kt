package com.example.demo.domain.usecase

import com.example.demo.domain.model.Photo
import com.example.demo.domain.repository.PhotoRepository
import javax.inject.Inject

class GetPhotoDetailsUseCase @Inject constructor(
    private val repository: PhotoRepository,
) {
    suspend operator fun invoke(id: String): Result<Photo> = repository.getPhoto(id)
}

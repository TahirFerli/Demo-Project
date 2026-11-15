package com.example.demo.data.mapper

import com.example.demo.data.remote.dto.ArtObjectDto
import com.example.demo.domain.model.Artwork

/**
 * Returns null when the object has no public-domain image: even with
 * `hasImages=true`, copyrighted works come back with an empty image URL.
 */
fun ArtObjectDto.toDomainOrNull(): Artwork? {
    val image = primaryImageSmall.ifBlank { primaryImage }
    if (image.isBlank()) return null
    return Artwork(
        id = objectID,
        title = title.nullIfBlank(),
        artist = artistDisplayName.nullIfBlank(),
        artistBio = artistDisplayBio.nullIfBlank(),
        date = objectDate.nullIfBlank(),
        medium = medium.nullIfBlank(),
        dimensions = dimensions.nullIfBlank(),
        department = department.nullIfBlank(),
        culture = culture.nullIfBlank(),
        creditLine = creditLine.nullIfBlank(),
        imageUrl = image,
        objectUrl = objectURL.nullIfBlank(),
    )
}

private fun String.nullIfBlank(): String? = takeIf { it.isNotBlank() }

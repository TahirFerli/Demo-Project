package com.example.demo.util

import com.example.demo.domain.model.Artwork
import com.example.demo.domain.model.ArtworkPage
import com.example.demo.domain.repository.ArtworkRepository

/** Hand-written fake: simpler and more readable than mocks for a small interface. */
class FakeArtworkRepository : ArtworkRepository {

    var artworksResult: (page: Int, pageSize: Int) -> Result<ArtworkPage> =
        { page, pageSize -> Result.success(ArtworkPage(testArtworks(page, pageSize), hasMore = true)) }
    var artworkResult: (id: Int) -> Result<Artwork> = { id -> Result.success(testArtwork(id)) }

    val requestedPages = mutableListOf<Int>()

    override suspend fun getArtworks(page: Int, pageSize: Int): Result<ArtworkPage> {
        requestedPages += page
        return artworksResult(page, pageSize)
    }

    override suspend fun getArtwork(id: Int): Result<Artwork> = artworkResult(id)
}

fun testArtwork(id: Int) = Artwork(
    id = id,
    title = "Title $id",
    artist = "Artist $id",
    artistBio = null,
    date = "1889",
    medium = "Oil on canvas",
    dimensions = null,
    department = "European Paintings",
    culture = null,
    creditLine = null,
    imageUrl = "image/$id",
    objectUrl = "https://www.metmuseum.org/art/collection/search/$id",
)

fun testArtworks(page: Int, pageSize: Int) =
    List(pageSize) { index -> testArtwork(id = page * pageSize + index) }

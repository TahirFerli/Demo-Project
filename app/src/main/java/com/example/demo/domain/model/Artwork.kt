package com.example.demo.domain.model

/**
 * Domain model. Free of serialization annotations so the UI never depends on
 * the shape of the network response. Optional fields are null instead of "".
 */
data class Artwork(
    val id: Int,
    val title: String?,
    val artist: String?,
    val artistBio: String?,
    val date: String?,
    val medium: String?,
    val dimensions: String?,
    val department: String?,
    val culture: String?,
    val creditLine: String?,
    /** Web-sized image (the API's "primaryImageSmall"). */
    val imageUrl: String,
    /** Page on metmuseum.org. */
    val objectUrl: String?,
)

/** One page of results plus whether more can be requested. */
data class ArtworkPage(
    val artworks: List<Artwork>,
    val hasMore: Boolean,
)

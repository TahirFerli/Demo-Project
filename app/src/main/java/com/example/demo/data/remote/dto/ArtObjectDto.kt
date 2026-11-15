package com.example.demo.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Subset of the object record we use. Defaults make parsing tolerant of
 * missing fields; unknown fields are ignored by the Json config.
 */
@Serializable
data class ArtObjectDto(
    val objectID: Int,
    val title: String = "",
    val artistDisplayName: String = "",
    val artistDisplayBio: String = "",
    val objectDate: String = "",
    val medium: String = "",
    val dimensions: String = "",
    val department: String = "",
    val culture: String = "",
    val creditLine: String = "",
    val primaryImage: String = "",
    val primaryImageSmall: String = "",
    val objectURL: String = "",
)

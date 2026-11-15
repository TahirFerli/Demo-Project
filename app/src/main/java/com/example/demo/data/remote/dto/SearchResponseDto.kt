package com.example.demo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val total: Int,
    // The API returns null instead of [] when nothing matches.
    val objectIDs: List<Int>? = null,
)

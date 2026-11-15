package com.example.demo.data.remote

import com.example.demo.data.remote.dto.ArtObjectDto
import com.example.demo.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for The Met Collection API (https://metmuseum.github.io, no API key).
 *
 * Search only returns IDs; each object must be fetched separately.
 */
interface MetMuseumApi {

    /** Paginated search (v1.1). `offset + limit` may not exceed [MAX_SEARCH_RESULTS]. */
    @GET("v1.1/search")
    suspend fun search(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("hasImages") hasImages: Boolean = true,
        @Query("isHighlight") isHighlight: Boolean = true,
    ): SearchResponseDto

    @GET("v1/objects/{id}")
    suspend fun getObject(@Path("id") id: Int): ArtObjectDto

    companion object {
        const val BASE_URL = "https://collectionapi.metmuseum.org/public/collection/"
        const val MAX_SEARCH_RESULTS = 10_000
    }
}

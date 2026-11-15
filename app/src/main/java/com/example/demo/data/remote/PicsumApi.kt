package com.example.demo.data.remote

import com.example.demo.data.remote.dto.PhotoDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit service for https://picsum.photos (no API key required). */
interface PicsumApi {

    @GET("v2/list")
    suspend fun getPhotos(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): List<PhotoDto>

    @GET("id/{id}/info")
    suspend fun getPhoto(@Path("id") id: String): PhotoDto

    companion object {
        const val BASE_URL = "https://picsum.photos/"
    }
}

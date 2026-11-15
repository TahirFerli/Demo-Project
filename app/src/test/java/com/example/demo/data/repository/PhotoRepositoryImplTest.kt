package com.example.demo.data.repository

import com.example.demo.data.remote.PicsumApi
import com.example.demo.domain.model.DataError
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Exercises the real Retrofit + kotlinx.serialization stack against a local
 * MockWebServer: verifies request paths, JSON parsing, mapping and error handling.
 */
class PhotoRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: PhotoRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(PicsumApi::class.java)
        repository = PhotoRepositoryImpl(api, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `getPhotos parses response and maps to domain`() = runTest {
        server.enqueue(MockResponse.Builder().body(LIST_JSON).build())

        val photos = repository.getPhotos(page = 2, pageSize = 30).getOrThrow()

        assertEquals("/v2/list?page=2&limit=30", server.takeRequest().target)
        assertEquals(1, photos.size)
        with(photos.first()) {
            assertEquals("0", id)
            assertEquals("Alejandro Escamilla", author)
            assertEquals("https://picsum.photos/id/0/400/266", thumbnailUrl)
            assertEquals("https://picsum.photos/id/0/5000/3333", fullImageUrl)
        }
    }

    @Test
    fun `http error is mapped to DataError Server`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).build())

        val error = repository.getPhotos(page = 1, pageSize = 30).exceptionOrNull()

        assertTrue(error is DataError.Server)
        assertEquals(503, (error as DataError.Server).code)
    }

    @Test
    fun `malformed json is mapped to DataError Unknown`() = runTest {
        server.enqueue(MockResponse.Builder().body("{ not json").build())

        val error = repository.getPhotos(page = 1, pageSize = 30).exceptionOrNull()

        assertTrue(error is DataError.Unknown)
    }

    @Test
    fun `getPhoto is served from cache after list load`() = runTest {
        server.enqueue(MockResponse.Builder().body(LIST_JSON).build())
        repository.getPhotos(page = 1, pageSize = 30)

        val photo = repository.getPhoto("0").getOrThrow()

        assertEquals("Alejandro Escamilla", photo.author)
        assertEquals(1, server.requestCount) // no second network call
    }

    private companion object {
        val LIST_JSON = """
            [{
              "id": "0",
              "author": "Alejandro Escamilla",
              "width": 5000,
              "height": 3333,
              "url": "https://unsplash.com/photos/yC-Yzbqy7PY",
              "download_url": "https://picsum.photos/id/0/5000/3333",
              "unexpected_field": true
            }]
        """.trimIndent()
    }
}

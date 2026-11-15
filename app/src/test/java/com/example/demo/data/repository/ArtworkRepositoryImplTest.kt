package com.example.demo.data.repository

import com.example.demo.data.remote.MetMuseumApi
import com.example.demo.domain.model.DataError
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Exercises the real Retrofit + kotlinx.serialization stack against a local
 * MockWebServer. Object requests run in parallel, so responses are routed by
 * path with a Dispatcher instead of a FIFO queue.
 */
class ArtworkRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: ArtworkRepositoryImpl

    /** path -> response; anything missing is a 404. */
    private val routes = mutableMapOf<String, MockResponse>()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                routes[request.url.encodedPath] ?: MockResponse.Builder().code(404).build()
        }
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(MetMuseumApi::class.java)
        repository = ArtworkRepositoryImpl(api, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() = server.close()

    private fun route(path: String, body: String, code: Int = 200) {
        routes[path] = MockResponse.Builder().code(code).body(body).build()
    }

    private fun searchReturns(total: Int, vararg ids: Int) =
        route("/v1.1/search", """{"total": $total, "objectIDs": [${ids.joinToString()}]}""")

    @Test
    fun `getArtworks searches then loads objects in order and maps them`() = runTest {
        searchReturns(total = 100, 1, 2, 3)
        route("/v1/objects/1", objectJson(1, title = "Wheat Field with Cypresses"))
        route("/v1/objects/2", objectJson(2))
        route("/v1/objects/3", objectJson(3))

        val page = repository.getArtworks(page = 1, pageSize = 3).getOrThrow()

        assertEquals(listOf(1, 2, 3), page.artworks.map { it.id })
        assertEquals("Wheat Field with Cypresses", page.artworks.first().title)
        assertEquals(null, page.artworks.first().culture) // "" mapped to null
        assertTrue(page.hasMore)
        val searchRequest = server.takeRequest()
        assertEquals(
            "/v1.1/search?offset=3&limit=3&hasImages=true&isHighlight=true",
            searchRequest.target,
        )
    }

    @Test
    fun `objects without an image are filtered out`() = runTest {
        searchReturns(total = 2, 1, 2)
        route("/v1/objects/1", objectJson(1))
        route("/v1/objects/2", objectJson(2, image = ""))

        val page = repository.getArtworks(page = 0, pageSize = 20).getOrThrow()

        assertEquals(listOf(1), page.artworks.map { it.id })
    }

    @Test
    fun `one failing object does not fail the whole page`() = runTest {
        searchReturns(total = 2, 1, 404)
        route("/v1/objects/1", objectJson(1))
        // /v1/objects/404 is not routed -> HTTP 404

        val result = repository.getArtworks(page = 0, pageSize = 20)

        assertEquals(listOf(1), result.getOrThrow().artworks.map { it.id })
    }

    @Test
    fun `page fails when every object request fails`() = runTest {
        searchReturns(total = 2, 1, 2)
        route("/v1/objects/1", "", code = 500)
        route("/v1/objects/2", "", code = 500)

        val error = repository.getArtworks(page = 0, pageSize = 20).exceptionOrNull()

        assertTrue(error is DataError.Server)
    }

    @Test
    fun `hasMore is false on the last page`() = runTest {
        searchReturns(total = 23, 21, 22, 23)
        (21..23).forEach { route("/v1/objects/$it", objectJson(it)) }

        val page = repository.getArtworks(page = 1, pageSize = 20).getOrThrow()

        assertFalse(page.hasMore)
    }

    @Test
    fun `search http error is mapped to DataError Server`() = runTest {
        route("/v1.1/search", "", code = 503)

        val error = repository.getArtworks(page = 0, pageSize = 20).exceptionOrNull()

        assertTrue(error is DataError.Server)
        assertEquals(503, (error as DataError.Server).code)
    }

    @Test
    fun `firewall 403 is mapped to DataError RateLimited`() = runTest {
        route("/v1.1/search", "<html>Request unsuccessful</html>", code = 403)

        val error = repository.getArtworks(page = 0, pageSize = 20).exceptionOrNull()

        assertTrue(error is DataError.RateLimited)
    }

    @Test
    fun `getArtwork is served from cache after list load`() = runTest {
        searchReturns(total = 1, 1)
        route("/v1/objects/1", objectJson(1))
        repository.getArtworks(page = 0, pageSize = 20)
        val requestsAfterList = server.requestCount

        val artwork = repository.getArtwork(1).getOrThrow()

        assertEquals(1, artwork.id)
        assertEquals(requestsAfterList, server.requestCount) // no extra network call
    }

    @Test
    fun `getArtwork 404 is mapped to DataError NotFound`() = runTest {
        val error = repository.getArtwork(999).exceptionOrNull()

        assertTrue(error is DataError.NotFound)
    }

    private fun objectJson(
        id: Int,
        title: String = "Title $id",
        image: String = "https://images.metmuseum.org/CRDImages/ep/web-large/$id.jpg",
    ) = """
        {
          "objectID": $id,
          "title": "$title",
          "artistDisplayName": "Vincent van Gogh",
          "objectDate": "1889",
          "medium": "Oil on canvas",
          "department": "European Paintings",
          "culture": "",
          "primaryImage": "$image",
          "primaryImageSmall": "$image",
          "objectURL": "https://www.metmuseum.org/art/collection/search/$id",
          "someNewField": {"ignored": true}
        }
    """.trimIndent()
}

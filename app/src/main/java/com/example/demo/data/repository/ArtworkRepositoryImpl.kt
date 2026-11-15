package com.example.demo.data.repository

import com.example.demo.core.suspendRunCatching
import com.example.demo.data.mapper.toDataError
import com.example.demo.data.mapper.toDomainOrNull
import com.example.demo.data.remote.MetMuseumApi
import com.example.demo.di.IoDispatcher
import com.example.demo.domain.model.Artwork
import com.example.demo.domain.model.ArtworkPage
import com.example.demo.domain.model.DataError
import com.example.demo.domain.repository.ArtworkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Met API returns only IDs from search, so a page is built in two steps:
 *   1. search -> list of object IDs (+ total for paging)
 *   2. fetch every object in PARALLEL (async/awaitAll), with a Semaphore capping
 *      concurrent requests to stay well below the API's 80 req/s limit.
 *
 * Loaded artworks are kept in an in-memory cache (guarded by a Mutex) so the
 * detail screen opens instantly for items already shown in the list.
 */
@Singleton
class ArtworkRepositoryImpl @Inject constructor(
    private val api: MetMuseumApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ArtworkRepository {

    private val requestLimiter = Semaphore(MAX_CONCURRENT_REQUESTS)
    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<Int, Artwork>()

    override suspend fun getArtworks(page: Int, pageSize: Int): Result<ArtworkPage> =
        withContext(ioDispatcher) {
            safeApiCall {
                val offset = page * pageSize
                val search = api.search(offset = offset, limit = pageSize)
                val ids = search.objectIDs.orEmpty()
                // Only the first 10,000 results of a query are reachable through search.
                val reachable = minOf(search.total, MetMuseumApi.MAX_SEARCH_RESULTS)
                ArtworkPage(
                    artworks = fetchObjects(ids),
                    hasMore = ids.isNotEmpty() && offset + pageSize < reachable,
                )
            }.onSuccess { result ->
                cacheMutex.withLock { result.artworks.forEach { cache[it.id] = it } }
            }
        }

    override suspend fun getArtwork(id: Int): Result<Artwork> =
        withContext(ioDispatcher) {
            cacheMutex.withLock { cache[id] }?.let { return@withContext Result.success(it) }
            safeApiCall {
                api.getObject(id).toDomainOrNull() ?: throw DataError.NotFound()
            }.onSuccess { artwork -> cacheMutex.withLock { cache[artwork.id] = artwork } }
        }

    /**
     * Fetches objects concurrently, keeping the original order.
     * One broken object (e.g. 404) must not fail the whole page, so each request is
     * wrapped individually. If EVERY request fails (e.g. offline), the page fails.
     */
    private suspend fun fetchObjects(ids: List<Int>): List<Artwork> = coroutineScope {
        val results = ids.map { id ->
            async {
                requestLimiter.withPermit { suspendRunCatching { api.getObject(id) } }
            }
        }.awaitAll()

        val loaded = results.mapNotNull { it.getOrNull() }
        if (loaded.isEmpty() && results.isNotEmpty()) {
            throw results.first().exceptionOrNull()!!
        }
        loaded.mapNotNull { it.toDomainOrNull() }
    }

    /** Wraps a network call and converts any failure into a domain [DataError]. */
    private inline fun <T> safeApiCall(block: () -> T): Result<T> =
        suspendRunCatching(block).fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toDataError()) },
        )

    private companion object {
        const val MAX_CONCURRENT_REQUESTS = 4
    }
}

package com.example.demo.data.repository

import com.example.demo.core.suspendRunCatching
import com.example.demo.data.mapper.toDomain
import com.example.demo.data.remote.PicsumApi
import com.example.demo.di.IoDispatcher
import com.example.demo.domain.model.Photo
import com.example.demo.domain.repository.PhotoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-backed repository with a small in-memory cache so the detail screen
 * can render instantly for photos already loaded in the list.
 *
 * - Main-safe: all work runs on the injected IO dispatcher (injected, not
 *   hard-coded, so tests can replace it).
 * - The cache is guarded by a [Mutex] because calls may run concurrently.
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val api: PicsumApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PhotoRepository {

    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<String, Photo>()

    override suspend fun getPhotos(page: Int, pageSize: Int): Result<List<Photo>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                api.getPhotos(page = page, limit = pageSize).map { it.toDomain() }
            }.onSuccess { photos ->
                cacheMutex.withLock { photos.forEach { cache[it.id] = it } }
            }
        }

    override suspend fun getPhoto(id: String): Result<Photo> =
        withContext(ioDispatcher) {
            cacheMutex.withLock { cache[id] }?.let { return@withContext Result.success(it) }
            suspendRunCatching { api.getPhoto(id).toDomain() }
                .onSuccess { photo -> cacheMutex.withLock { cache[photo.id] = photo } }
        }
}

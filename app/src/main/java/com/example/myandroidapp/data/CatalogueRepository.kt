package com.example.myandroidapp.data

import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory catalogue cache. A single paginated API call returns a complete
 * batch hierarchy, avoiding per-course/per-chapter network fan-out.
 */
object CatalogueRepository {
    private const val PAGE_SIZE = 50
    private const val CACHE_TTL_MS = 2 * 60 * 1000L
    private val mutex = Mutex()
    private var cached: CatalogueSnapshotDto? = null
    private var cachedAt = 0L

    suspend fun load(api: LearningApi, forceRefresh: Boolean = false): CatalogueSnapshotDto =
        mutex.withLock {
            val now = SystemClock.elapsedRealtime()
            cached?.takeIf { !forceRefresh && now - cachedAt < CACHE_TTL_MS }?.let { return it }

            val batches = mutableListOf<CatalogueBatchDto>()
            var firstPage: CataloguePageDto? = null
            var page = 1
            do {
                val response = api.catalogue(page, PAGE_SIZE)
                if (firstPage == null) firstPage = response
                batches += response.items
                if (!response.hasNextPage) break
                page += 1
                require(page <= response.totalPages + 1) { "Catalogue pagination did not terminate." }
            } while (true)

            val metadata = requireNotNull(firstPage) { "Catalogue response was empty." }
            val snapshot = CatalogueSnapshotDto(
                batches = batches.distinctBy { it.id },
                liveClasses = metadata.liveClasses,
                legacyLive = metadata.legacyLive,
                legacyRecorded = metadata.legacyRecorded,
                subjects = metadata.subjects,
                schedules = metadata.schedules
            )
            cached = snapshot
            cachedAt = SystemClock.elapsedRealtime()
            snapshot
        }

    fun invalidate() {
        cached = null
        cachedAt = 0L
    }
}

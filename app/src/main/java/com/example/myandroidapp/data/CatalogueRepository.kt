package com.example.myandroidapp.data

import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory catalogue cache. A single paginated API call returns a complete
 * batch hierarchy, avoiding per-course/per-chapter network fan-out.
 */

internal class CataloguePageAccumulator {
    private val batches = linkedMapOf<String, CatalogueBatchDto>()
    private var expectedPage = 1
    private var expectedPageSize: Int? = null
    private var finished = false
    private var first: CataloguePageDto? = null

    fun append(response: CataloguePageDto) {
        require(!finished) { "Catalogue pagination is already complete." }
        require(response.page == expectedPage) { "Expected catalogue page $expectedPage." }
        require(response.pageSize > 0) { "Catalogue pageSize must be positive." }
        expectedPageSize?.let { require(it == response.pageSize) { "Catalogue pageSize changed during pagination." } }
        expectedPageSize = response.pageSize
        if (first == null) first = response
        response.items.forEach { batch -> batches.putIfAbsent(batch.id, batch) }
        finished = !response.hasNextPage
        expectedPage += 1
        require(expectedPage <= response.totalPages + 1) { "Catalogue pagination exceeded totalPages." }
    }

    fun snapshot(): CatalogueSnapshotDto {
        require(finished) { "Catalogue pages are incomplete." }
        val metadata = requireNotNull(first) { "Catalogue contains no pages." }
        return CatalogueSnapshotDto(
            batches = batches.values.toList(),
            liveClasses = metadata.liveClasses,
            legacyLive = metadata.legacyLive,
            legacyRecorded = metadata.legacyRecorded,
            subjects = metadata.subjects,
            schedules = metadata.schedules
        )
    }
}

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

            val accumulator = CataloguePageAccumulator()
            var page = 1
            do {
                val response = api.catalogue(page, PAGE_SIZE)
                accumulator.append(response)
                if (!response.hasNextPage) break
                page += 1
            } while (true)

            val snapshot = accumulator.snapshot()
            cached = snapshot
            cachedAt = SystemClock.elapsedRealtime()
            snapshot
        }

    fun invalidate() {
        cached = null
        cachedAt = 0L
    }
}

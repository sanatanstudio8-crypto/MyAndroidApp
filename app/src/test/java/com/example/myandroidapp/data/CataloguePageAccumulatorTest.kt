package com.example.myandroidapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CataloguePageAccumulatorTest {
    @Test fun accumulatesPagesInStableOrderAndKeepsVisibilityAndHierarchy() {
        val first = CataloguePageDto(
            items = listOf(CatalogueBatchDto("b1", "Batch 1", courses = listOf(
                CatalogueCourseDto("c1", "Course 1", visibility = "free", chapters = listOf(
                    CatalogueChapterDto("ch1", "c1", "Chapter 1", lectures = listOf(
                        CatalogueLectureDto("l1", "ch1", "Lecture 1", visibility = "free")
                    ))
                ))
            ))),
            page = 1, pageSize = 1, totalItems = 2, totalPages = 2, hasNextPage = true
        )
        val second = CataloguePageDto(
            items = listOf(CatalogueBatchDto("b2", "Batch 2", courses = listOf(
                CatalogueCourseDto("c2", "Course 2", visibility = "paid")
            ))),
            page = 2, pageSize = 1, totalItems = 2, totalPages = 2, hasNextPage = false
        )
        val accumulator = CataloguePageAccumulator()
        accumulator.append(first)
        accumulator.append(second)
        val result = accumulator.snapshot()
        assertEquals(listOf("b1", "b2"), result.batches.map { it.id })
        assertEquals("free", result.batches[0].courses[0].visibility)
        assertEquals("l1", result.batches[0].courses[0].chapters[0].lectures[0].id)
        assertEquals("paid", result.batches[1].courses[0].visibility)
    }

    @Test fun rejectsOutOfOrderPages() {
        val accumulator = CataloguePageAccumulator()
        val page = CataloguePageDto(page = 2, pageSize = 10, totalItems = 20, totalPages = 2, hasNextPage = false)
        assertThrows(IllegalArgumentException::class.java) { accumulator.append(page) }
    }

    @Test fun acceptsAnEmptyCatalogue() {
        val accumulator = CataloguePageAccumulator()
        accumulator.append(CataloguePageDto(page = 1, pageSize = 50, totalItems = 0, totalPages = 0, hasNextPage = false))
        assertEquals(emptyList<String>(), accumulator.snapshot().batches.map { it.id })
    }

    @Test fun rejectsIncompleteSnapshot() {
        val accumulator = CataloguePageAccumulator()
        accumulator.append(CataloguePageDto(page = 1, pageSize = 10, totalItems = 20, totalPages = 2, hasNextPage = true))
        assertThrows(IllegalArgumentException::class.java) { accumulator.snapshot() }
    }
}

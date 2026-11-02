package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingProgressTest {

    @Test
    fun `pdf progress counts the visible page as read`() {
        assertEquals(0.01f, ReadingProgress.forPdf(pageIndex = 0, pageCount = 100), 1e-6f)
        assertEquals(0.5f, ReadingProgress.forPdf(pageIndex = 49, pageCount = 100), 1e-6f)
        assertEquals(1f, ReadingProgress.forPdf(pageIndex = 99, pageCount = 100), 1e-6f)
    }

    @Test
    fun `pdf progress is safe for empty or out of range input`() {
        assertEquals(0f, ReadingProgress.forPdf(pageIndex = 3, pageCount = 0), 0f)
        assertEquals(1f, ReadingProgress.forPdf(pageIndex = 500, pageCount = 10), 0f)
    }

    @Test
    fun `epub progress clamps and handles null`() {
        assertEquals(0f, ReadingProgress.forEpub(null), 0f)
        assertEquals(0.42f, ReadingProgress.forEpub(0.42), 1e-6f)
        assertEquals(1f, ReadingProgress.forEpub(1.3), 0f)
    }

    @Test
    fun `percent never shows 100 before the book is done`() {
        assertEquals(0, ReadingProgress.percent(0f))
        assertEquals(42, ReadingProgress.percent(0.42f))
        assertEquals(99, ReadingProgress.percent(0.994f))
        assertEquals(100, ReadingProgress.percent(0.996f))
        assertFalse(ReadingProgress.isDone(0.99f))
        assertTrue(ReadingProgress.isDone(1f))
    }
}

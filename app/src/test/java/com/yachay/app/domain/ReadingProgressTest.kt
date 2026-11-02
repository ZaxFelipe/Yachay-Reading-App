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
    fun `merge keeps the furthest point read`() {
        assertEquals(1f, ReadingProgress.merge(stored = 1f, current = 0.97f), 0f)
        assertEquals(0.5f, ReadingProgress.merge(stored = 0.4f, current = 0.5f), 0f)
        assertTrue(ReadingProgress.isDone(ReadingProgress.merge(stored = 1f, current = 0.1f)))
    }

    @Test
    fun `merge clamps the current value`() {
        assertEquals(1f, ReadingProgress.merge(stored = 0.2f, current = 1.4f), 0f)
        assertEquals(0.2f, ReadingProgress.merge(stored = 0.2f, current = -1f), 0f)
    }

    @Test
    fun `finished book stays done after going back a page`() {
        // Cenário reproduzido no celular: 30/30, volta para 29/30.
        val finished = ReadingProgress.forPdf(pageIndex = 29, pageCount = 30)
        val back = ReadingProgress.forPdf(pageIndex = 28, pageCount = 30)
        assertTrue(ReadingProgress.isDone(ReadingProgress.merge(finished, back)))
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

    @Test
    fun `cbz pages count like pdf pages`() {
        (0 until 30).forEach { assertEquals(ReadingProgress.forPdf(it, 30), ReadingProgress.forPages(it, 30), 0f) }
        assertTrue(ReadingProgress.isDone(ReadingProgress.forPages(pageIndex = 23, pageCount = 24)))
        assertEquals(0f, ReadingProgress.forPages(0, 0), 0f)
    }
}

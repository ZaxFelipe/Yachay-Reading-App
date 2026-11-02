package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyGoalTest {

    private val today = LocalDate.of(2026, 11, 2)

    @Test
    fun `default goal is 10 pages and almost there starts at 60 percent`() {
        assertEquals(10, DailyGoal.DEFAULT_TARGET)
        assertEquals(6, DailyGoal.almostThereAt(10))
        // Arredonda para cima: 60% de 7 = 4,2 → 5 páginas.
        assertEquals(5, DailyGoal.almostThereAt(7))
    }

    @Test
    fun `progress fraction and states`() {
        assertEquals(0f, DailyGoalProgress(0).fraction, 0f)
        assertEquals(0.6f, DailyGoalProgress(6).fraction, 1e-6f)
        assertEquals(1f, DailyGoalProgress(14).fraction, 0f)
        assertFalse(DailyGoalProgress(5).isAlmostThere)
        assertTrue(DailyGoalProgress(6).isAlmostThere)
        assertFalse(DailyGoalProgress(10).isAlmostThere)
        assertTrue(DailyGoalProgress(10).isDone)
    }

    @Test
    fun `almost there fires only when crossing 60 percent`() {
        assertTrue(DailyGoal.crossesAlmostThere(before = 5, after = 6, target = 10))
        assertFalse(DailyGoal.crossesAlmostThere(before = 6, after = 7, target = 10))
        assertFalse(DailyGoal.crossesAlmostThere(before = 4, after = 5, target = 10))
        // Meta tão pequena que 60% já é a meta inteira: não há "quase".
        assertFalse(DailyGoal.crossesAlmostThere(before = 0, after = 1, target = 1))
    }

    @Test
    fun `target is reached only on the page that completes it`() {
        assertTrue(DailyGoal.reachesTarget(before = 9, after = 10, target = 10))
        assertFalse(DailyGoal.reachesTarget(before = 10, after = 11, target = 10))
        assertFalse(DailyGoal.reachesTarget(before = 8, after = 9, target = 10))
    }

    @Test
    fun `the same page counts once per day`() {
        var day = DailyGoal.record(null, today, DailyGoal.pageKey(bookId = 1, page = "p3"))
        day = DailyGoal.record(day, today, DailyGoal.pageKey(bookId = 1, page = "p3"))
        day = DailyGoal.record(day, today, DailyGoal.pageKey(bookId = 1, page = "p4"))
        // Mesma posição em outro livro é outra página.
        day = DailyGoal.record(day, today, DailyGoal.pageKey(bookId = 2, page = "p3"))
        assertEquals(3, day.pages.size)
    }

    @Test
    fun `epub screens are told apart by chapter and progression`() {
        val a = DailyGoal.epubPage("cap1.xhtml", 0.0343)
        assertEquals(a, DailyGoal.epubPage("cap1.xhtml", 0.03430001))
        assertTrue(a != DailyGoal.epubPage("cap1.xhtml", 0.0381))
        assertTrue(a != DailyGoal.epubPage("cap2.xhtml", 0.0343))
    }

    @Test
    fun `a new day starts from zero`() {
        val yesterday = DailyGoal.record(null, today.minusDays(1), DailyGoal.pageKey(1, "p1"))
        assertEquals(0, DailyGoal.pagesOn(yesterday, today))
        val day = DailyGoal.record(yesterday, today, DailyGoal.pageKey(1, "p1"))
        assertEquals(today, day.date)
        assertEquals(1, DailyGoal.pagesOn(day, today))
    }
}

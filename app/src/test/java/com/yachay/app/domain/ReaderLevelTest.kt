package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLevelTest {

    @Test
    fun `each level costs as many goals as its number`() {
        // Nível 2 com 1 meta, 3 com mais 2, 4 com mais 3...
        assertEquals(listOf(1, 2, 2, 3, 3, 3, 4), (0..6).map { ReaderStats(goalsMet = it).level })
        assertEquals(0, ReaderLevel.xpFor(1))
        assertEquals(12_000, ReaderLevel.xpFor(ReaderLevel.MAX_LEVEL))
    }

    @Test
    fun `xp bar shows progress inside the current level`() {
        val stats = ReaderStats(goalsMet = 4) // 400 XP: nível 3 (300) rumo ao 4 (600)
        assertEquals(3, stats.level)
        assertEquals(100, stats.xpInLevel)
        assertEquals(300, stats.xpForNextLevel)
        assertEquals(1f / 3f, stats.levelFraction, 0.001f)
        assertEquals(2, stats.goalsToNextLevel)
    }

    @Test
    fun `max level stops at the last avatar`() {
        val stats = ReaderStats(goalsMet = 500)
        assertEquals(ReaderLevel.MAX_LEVEL, stats.level)
        assertTrue(stats.isMaxLevel)
        assertEquals(1f, stats.levelFraction)
        assertEquals(0, stats.goalsToNextLevel)
        assertFalse(ReaderStats(goalsMet = 119).isMaxLevel)
    }

    @Test
    fun `level comes from the stored xp, not from the goal count`() {
        // 2 metas de 20 páginas (400 XP) valem o mesmo que 4 metas de 10.
        val stats = ReaderStats(goalsMet = 2, xp = 400, goalTarget = 20)
        assertEquals(3, stats.level)
        // Faltam 200 XP para o nível 4: uma meta de 20 páginas.
        assertEquals(1, stats.goalsToNextLevel)
        assertEquals(4, ReaderStats(goalsMet = 2, xp = 400, goalTarget = 5).goalsToNextLevel)
    }
}

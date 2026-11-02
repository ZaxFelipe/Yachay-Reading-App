package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakTest {

    private val day = LocalDate.of(2026, 11, 2)

    @Test
    fun `consecutive days add up and the same day does not`() {
        var last: LocalDate? = null
        var count = 0
        (0L until 3L).forEach {
            val today = day.plusDays(it)
            count = Streak.onGoalMet(last, count, today)
            last = today
        }
        assertEquals(3, count)
        assertEquals(3, Streak.onGoalMet(last, count, last!!))
    }

    @Test
    fun `a missed day shows zero and the next goal restarts at one`() {
        // Bateu dia 2; dia 3 sem meta; dia 4 a sequência mostrada é 0 mesmo sem abrir o app.
        assertEquals(5, Streak.current(day, 5, day.plusDays(1)))
        assertEquals(0, Streak.current(day, 5, day.plusDays(2)))
        assertEquals(1, Streak.onGoalMet(day, 5, day.plusDays(2)))
    }

    @Test
    fun `month and year boundaries are just consecutive days`() {
        assertEquals(4, Streak.onGoalMet(LocalDate.of(2026, 10, 31), 3, LocalDate.of(2026, 11, 1)))
        assertEquals(8, Streak.onGoalMet(LocalDate.of(2026, 12, 31), 7, LocalDate.of(2027, 1, 1)))
        assertEquals(2, Streak.onGoalMet(LocalDate.of(2028, 2, 28), 1, LocalDate.of(2028, 2, 29)))
        assertEquals(1, Streak.onGoalMet(LocalDate.of(2027, 2, 27), 1, LocalDate.of(2027, 3, 1)))
    }

    @Test
    fun `bonus every seven days`() {
        assertEquals(listOf(7, 14, 21), (0..21).filter(Streak::isBonus))
        assertFalse(Streak.isBonus(0))
        assertTrue(Streak.isBonus(28))
    }
}

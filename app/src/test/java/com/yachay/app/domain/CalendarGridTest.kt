package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CalendarGridTest {

    @Test
    fun `weeks start on sunday and end with the current week`() {
        val today = LocalDate.of(2026, 11, 4) // quarta
        val weeks = CalendarGrid.weeks(today, firstDay = null)
        assertEquals(CalendarGrid.MIN_WEEKS, weeks.size)
        weeks.forEach { week ->
            assertEquals(7, week.size)
            assertEquals(DayOfWeek.SUNDAY, week.first().dayOfWeek)
            assertEquals(DayOfWeek.SATURDAY, week.last().dayOfWeek)
        }
        assertEquals(LocalDate.of(2026, 11, 1), weeks.last().first())
        assertEquals(today, weeks.last()[3])
    }

    @Test
    fun `sunday and saturday belong to their own week`() {
        assertEquals(LocalDate.of(2026, 11, 1), CalendarGrid.weekStart(LocalDate.of(2026, 11, 1)))
        assertEquals(LocalDate.of(2026, 11, 1), CalendarGrid.weekStart(LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun `grid crosses the year and grows to reach the first day`() {
        val today = LocalDate.of(2027, 1, 2) // sábado
        val weeks = CalendarGrid.weeks(today, firstDay = LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2025, 12, 28), weeks.first().first())
        assertEquals(LocalDate.of(2026, 12, 27), weeks.last().first())
        assertEquals(today, weeks.last().last())
        // Dias seguidos, sem buraco nem repetição.
        val days = weeks.flatten()
        days.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun `leap day is in the grid`() {
        val weeks = CalendarGrid.weeks(LocalDate.of(2028, 3, 1), firstDay = null)
        val feb = weeks.flatten().filter { it.year == 2028 && it.monthValue == 2 }
        assertEquals(29, feb.size)
    }

    @Test
    fun `month labels start at the first sunday of each month`() {
        val weeks = CalendarGrid.weeks(LocalDate.of(2026, 11, 4), firstDay = null)
        val starts = CalendarGrid.monthStarts(weeks)
        assertEquals(0, starts.first())
        // 17 semanas até novembro: julho (1ª coluna), agosto, setembro, outubro e novembro.
        assertEquals(5, starts.size)
        starts.drop(1).forEach { assertEquals(true, weeks[it].first().dayOfMonth <= 7) }
    }
}

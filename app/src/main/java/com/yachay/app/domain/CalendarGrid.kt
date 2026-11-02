package com.yachay.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Um dia do histórico: páginas contadas pela meta e se ela foi batida. */
data class CalendarDay(val pages: Int, val goalMet: Boolean)

/**
 * Grade do calendário do Perfil, como o gráfico de contribuições do GitHub: uma coluna por
 * semana (domingo → sábado), da mais antiga à atual.
 */
object CalendarGrid {
    /** Semanas mostradas mesmo sem histórico (~4 meses); com mais histórico, a grade cresce. */
    const val MIN_WEEKS = 17

    /** Domingo da semana de [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

    /**
     * Cada semana com as 7 datas (dom → sáb). A última inclui dias depois de [today], que a
     * tela deixa em branco. [firstDay] = começo do histórico, para a grade ir até lá.
     */
    fun weeks(today: LocalDate, firstDay: LocalDate?): List<List<LocalDate>> {
        val last = weekStart(today)
        val sinceFirst = firstDay?.let { ChronoUnit.WEEKS.between(weekStart(it), last).toInt() + 1 } ?: 0
        val count = maxOf(MIN_WEEKS, sinceFirst)
        val first = last.minusWeeks((count - 1).toLong())
        return List(count) { week -> List(7) { day -> first.plusWeeks(week.toLong()).plusDays(day.toLong()) } }
    }

    /** Colunas onde começa um mês (a primeira semana cujo domingo já é do mês novo), para os rótulos. */
    fun monthStarts(weeks: List<List<LocalDate>>): List<Int> =
        weeks.indices.filter { i -> i == 0 || weeks[i].first().month != weeks[i - 1].first().month }
}

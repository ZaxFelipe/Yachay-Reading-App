package com.yachay.app.domain

import java.time.LocalDate
import kotlin.math.roundToInt

/** Páginas distintas lidas num dia (chaves de [DailyGoal.pageKey]). */
data class ReadingDay(val date: LocalDate, val pages: Set<String>)

/** Andamento da meta de hoje. */
data class DailyGoalProgress(val pagesRead: Int, val target: Int = DailyGoal.DEFAULT_TARGET) {
    val fraction: Float get() = if (target <= 0) 1f else (pagesRead.toFloat() / target).coerceIn(0f, 1f)
    val isDone: Boolean get() = pagesRead >= target
    val isAlmostThere: Boolean get() = !isDone && pagesRead >= DailyGoal.almostThereAt(target)
}

/**
 * Meta diária de leitura. Uma "página" é uma tela em que a pessoa ficou (PDF: a página;
 * EPUB: o ponto do capítulo onde a tela começa, ver [epubPage]), contada uma vez por dia.
 */
object DailyGoal {
    /** Fixa por enquanto; pode virar preferência no futuro. */
    const val DEFAULT_TARGET = 10
    /** "Quase lá" a partir de 60% da meta. */
    private const val ALMOST_THERE_PERCENT = 60

    /** Tempo mínimo numa página para contá-la como lida (folhear rápido não conta). */
    const val PAGE_DWELL_MS = 3_000L

    /** Em inteiros, arredondando para cima (0.6f em float daria 6.0000002 → 7). */
    fun almostThereAt(target: Int): Int = (target * ALMOST_THERE_PERCENT + 99) / 100

    /** O aviso "quase lá" aparece uma vez: na página que cruza os 60%, sem já ter batido a meta. */
    fun crossesAlmostThere(before: Int, after: Int, target: Int): Boolean {
        val at = almostThereAt(target)
        return before < at && after >= at && after < target
    }

    /** A meta é batida uma vez: na página que completa o alvo. */
    fun reachesTarget(before: Int, after: Int, target: Int): Boolean = before < target && after >= target

    fun pageKey(bookId: Long, page: String): String = "$bookId:$page"

    fun pdfPage(index: Int): String = "p$index"

    /**
     * Tela do EPUB: capítulo + `progression` onde a tela começa. A `position` do Readium não
     * serve: num livro real uma posição cobre várias telas. Mudar a fonte gera telas novas.
     */
    fun epubPage(href: String, progression: Double): String = "$href@${(progression * 10_000).roundToInt()}"

    /** Registra a página no dia; outro dia recomeça do zero. */
    fun record(stored: ReadingDay?, today: LocalDate, key: String): ReadingDay {
        val pages = if (stored?.date == today) stored.pages else emptySet()
        return ReadingDay(today, pages + key)
    }

    fun pagesOn(stored: ReadingDay?, today: LocalDate): Int =
        if (stored?.date == today) stored.pages.size else 0
}

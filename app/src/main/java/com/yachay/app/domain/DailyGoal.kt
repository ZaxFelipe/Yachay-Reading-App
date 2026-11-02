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
    /** Alvo padrão; a pessoa escolhe outro em Ajustes ([TARGETS]). */
    const val DEFAULT_TARGET = 10
    val TARGETS = listOf(5, 10, 20, 30)

    /** XP por página da meta: meta de N páginas vale N × 10 (reduzir a meta não é atalho). */
    const val XP_PER_TARGET_PAGE = 10

    /** Mais que isso a barra fica fina demais: os setores passam a agrupar páginas. */
    private const val MAX_SEGMENTS = 10
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

    /** Alvo gravado inválido (ausente ou fora das opções) volta ao padrão. */
    fun sanitizeTarget(stored: Int?): Int = stored?.takeIf { it in TARGETS } ?: DEFAULT_TARGET

    fun xpFor(target: Int): Int = target * XP_PER_TARGET_PAGE

    /** Páginas por setor da barra: 1 até 10 páginas; acima disso, o menor grupo que cabe em 10 setores. */
    fun pagesPerSegment(target: Int): Int = maxOf(1, (target + MAX_SEGMENTS - 1) / MAX_SEGMENTS)

    fun segments(target: Int): Int = (target + pagesPerSegment(target) - 1) / pagesPerSegment(target)

    /** Setores cheios: só conta setor completo (2 de 3 páginas ainda não pinta). */
    fun filledSegments(pagesRead: Int, target: Int): Int =
        (pagesRead.coerceAtMost(target) / pagesPerSegment(target)).coerceAtMost(segments(target))

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

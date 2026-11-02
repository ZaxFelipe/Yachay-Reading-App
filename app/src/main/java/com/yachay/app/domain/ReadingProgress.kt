package com.yachay.app.domain

import kotlin.math.roundToInt

/** Regras de progresso de leitura. O progresso é sempre armazenado como fração 0f..1f. */
object ReadingProgress {

    /** Considera o livro concluído a partir deste ponto (EPUBs raramente chegam a 1.0 exato). */
    const val DONE_THRESHOLD = 0.995f

    /** PDF: página atual (0-based) sobre o total, contando a página visível como lida. */
    fun forPdf(pageIndex: Int, pageCount: Int): Float {
        if (pageCount <= 0) return 0f
        return ((pageIndex + 1).toFloat() / pageCount).coerceIn(0f, 1f)
    }

    /** EPUB: `Locator.locations.totalProgression` do Readium (pode ser nulo no início). */
    fun forEpub(totalProgression: Double?): Float =
        (totalProgression ?: 0.0).toFloat().coerceIn(0f, 1f)

    fun isDone(progress: Float): Boolean = progress >= DONE_THRESHOLD

    /** Percentual inteiro para exibição; nunca mostra 100% antes de concluir de fato. */
    fun percent(progress: Float): Int {
        if (isDone(progress)) return 100
        return (progress * 100).roundToInt().coerceIn(0, 99)
    }
}

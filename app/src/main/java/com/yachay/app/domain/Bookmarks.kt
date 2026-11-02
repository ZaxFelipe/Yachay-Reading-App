package com.yachay.app.domain

/** Ponto de um EPUB extraído do `Locator`: capítulo, posição nele e no livro. */
data class EpubSpot(val href: String, val progression: Double, val totalProgression: Double? = null)

/** Regras dos marcadores. */
object Bookmarks {
    /**
     * A tela atual tem marcador se ele começa no mesmo ponto do capítulo (o mesmo arredondamento
     * da meta, [DailyGoal.epubPage]). Mudar a fonte gera telas novas: o marcador continua na
     * lista e levando ao lugar certo, só o ícone da tela deixa de acender.
     */
    fun sameScreen(a: EpubSpot, b: EpubSpot): Boolean =
        DailyGoal.epubPage(a.href, a.progression) == DailyGoal.epubPage(b.href, b.progression)

    /** Ordem de leitura na lista: pela posição no livro, desempatando pelo capítulo e pelo ponto nele. */
    val EpubOrder: Comparator<EpubSpot> =
        compareBy<EpubSpot> { it.totalProgression ?: Double.MAX_VALUE }.thenBy { it.href }.thenBy { it.progression }
}

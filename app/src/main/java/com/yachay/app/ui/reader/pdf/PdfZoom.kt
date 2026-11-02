package com.yachay.app.ui.reader.pdf

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Página visível na lista (índice, topo e altura em px, nas coordenadas da lista sem zoom). */
data class PageSlot(val index: Int, val offset: Int, val size: Int)

/**
 * Matemática do zoom do leitor de PDF. A transformação é `tela = escala × local + deslocamento`,
 * com origem no canto superior esquerdo da lista. Funções puras, testadas em `PdfZoomTest`.
 */
object PdfZoom {
    const val MIN = 1f
    const val MAX = 5f
    const val DOUBLE_TAP = 2.5f

    /** Re-render só renderiza até este múltiplo da largura da tela (acima disso o ganho é pouco). */
    const val MAX_RENDER_SCALE = 2.5f
    const val MAX_RENDER_WIDTH_PX = 4096

    fun clampZoom(scale: Float, min: Float = MIN, max: Float = MAX): Float =
        if (scale.isNaN()) min else scale.coerceIn(min, max)

    /** Faixa válida do deslocamento num eixo: o conteúdo escalado sempre cobre a tela. */
    fun offsetLimits(scale: Float, viewport: Float, content: Float = viewport): ClosedFloatingPointRange<Float> =
        min(0f, viewport - content * scale)..0f

    /** Novo deslocamento para que o ponto sob [focus] continue no mesmo lugar ao ir de [oldScale] para [newScale]. */
    fun offsetForZoom(offset: Float, focus: Float, oldScale: Float, newScale: Float): Float =
        focus - (focus - offset) * (newScale / oldScale)

    fun renderWidthFor(viewportPx: Int, scale: Float, maxPx: Int): Int =
        (viewportPx * scale).roundToInt().coerceAtMost(maxPx).coerceAtLeast(viewportPx).coerceAtLeast(1)

    /** Maior largura cuja página (ARGB_8888, 4 bytes/px) cabe em [maxBytes]. */
    fun memoryCapWidth(maxBytes: Long, aspectRatio: Float): Int =
        sqrt(maxBytes / 4.0 * aspectRatio).toInt()

    /** Centro da área visível da tela, nas coordenadas da lista (sem zoom). */
    fun visibleCenter(viewportStart: Int, viewportHeight: Int, scale: Float, offsetY: Float): Int =
        viewportStart + ((viewportHeight / 2f - offsetY) / scale).roundToInt()

    /**
     * Página atual = a que cruza o centro visível. No fim da lista conta a última (ela pode ser
     * curta demais para alcançar o centro).
     */
    fun pageAtCenter(pages: List<PageSlot>, center: Int, atEnd: Boolean, lastIndex: Int, fallback: Int): Int = when {
        atEnd -> lastIndex
        else -> pages.firstOrNull { it.offset <= center && it.offset + it.size >= center }?.index ?: fallback
    }
}

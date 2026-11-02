package com.yachay.app.ui.reader

import android.graphics.Bitmap

/**
 * Livro feito de páginas desenhadas (PDF, CBZ): o leitor de páginas (lista + zoom + re-render na
 * largura do zoom) só depende disto.
 */
interface PageSource {
    /** largura/altura de cada página, para reservar o espaço antes de renderizar. */
    val pageAspectRatios: List<Float>

    val pageCount: Int get() = pageAspectRatios.size

    /** Maior largura de re-render (zoom) para a página, respeitando um teto de memória. */
    fun maxRenderWidth(index: Int): Int

    suspend fun render(index: Int, widthPx: Int): Bitmap?

    suspend fun close()
}

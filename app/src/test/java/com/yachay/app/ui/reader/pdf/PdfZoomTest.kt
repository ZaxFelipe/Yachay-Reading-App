package com.yachay.app.ui.reader.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfZoomTest {

    @Test
    fun `zoom is clamped between 1x and 5x`() {
        assertEquals(1f, PdfZoom.clampZoom(0.4f), 0f)
        assertEquals(3f, PdfZoom.clampZoom(3f), 0f)
        assertEquals(5f, PdfZoom.clampZoom(9f), 0f)
        assertEquals(1f, PdfZoom.clampZoom(Float.NaN), 0f)
    }

    @Test
    fun `offset never shows beyond the page edge`() {
        // 1×: sem folga nenhuma.
        assertEquals(0f..0f, PdfZoom.offsetLimits(scale = 1f, viewport = 1000f))
        // 2×: o conteúdo tem 2000 px; dá para deslocar até -1000 (borda direita/de baixo).
        assertEquals(-1000f..0f, PdfZoom.offsetLimits(scale = 2f, viewport = 1000f))
        // Conteúdo menor que a tela não se desloca.
        assertEquals(0f..0f, PdfZoom.offsetLimits(scale = 1f, viewport = 1000f, content = 500f))
    }

    @Test
    fun `zooming keeps the point under the fingers in place`() {
        val focus = 300f
        val before = 0f
        val after = PdfZoom.offsetForZoom(offset = before, focus = focus, oldScale = 1f, newScale = 2f)
        // Ponto local sob o foco: (300 - 0) / 1 = 300 → depois do zoom continua em 300 na tela.
        assertEquals(focus, 300f * 2f + after, 1e-3f)
    }

    @Test
    fun `render width respects the ceiling`() {
        assertEquals(1000, PdfZoom.renderWidthFor(viewportPx = 1000, scale = 1f, maxPx = 4096))
        assertEquals(2500, PdfZoom.renderWidthFor(viewportPx = 1000, scale = 2.5f, maxPx = 4096))
        assertEquals(4096, PdfZoom.renderWidthFor(viewportPx = 1000, scale = 5f, maxPx = 4096))
        // O teto nunca deixa renderizar abaixo da largura da tela.
        assertEquals(1000, PdfZoom.renderWidthFor(viewportPx = 1000, scale = 3f, maxPx = 600))
    }

    @Test
    fun `memory cap keeps one page bitmap under the budget`() {
        val budget = 40L * 1024 * 1024
        val width = PdfZoom.memoryCapWidth(maxBytes = budget, aspectRatio = 0.7f)
        val height = width / 0.7f
        assertTrue("página de $width px passa do orçamento", width * height * 4 <= budget)
    }

    @Test
    fun `current page is the one crossing the screen center`() {
        // Páginas de 1000 px com 20 px de espaço, a lista começa no topo.
        val pages = listOf(PageSlot(0, offset = 0, size = 1000), PageSlot(1, offset = 1020, size = 1000))
        val center1x = PdfZoom.visibleCenter(viewportStart = 0, viewportHeight = 1600, scale = 1f, offsetY = 0f)
        assertEquals(800, center1x)
        assertEquals(0, PdfZoom.pageAtCenter(pages, center1x, atEnd = false, lastIndex = 9, fallback = 0))
    }

    @Test
    fun `current page follows the zoomed window, not the list center`() {
        val pages = listOf(PageSlot(0, offset = 0, size = 1000), PageSlot(1, offset = 1020, size = 1000))
        // 2× com a janela na metade de baixo da lista: o centro da tela está em (800 + 1600) / 2 = 1200.
        val center = PdfZoom.visibleCenter(viewportStart = 0, viewportHeight = 1600, scale = 2f, offsetY = -1600f)
        assertEquals(1200, center)
        assertEquals(1, PdfZoom.pageAtCenter(pages, center, atEnd = false, lastIndex = 9, fallback = 0))
    }

    @Test
    fun `end of the list counts as the last page`() {
        val pages = listOf(PageSlot(8, offset = 0, size = 1000), PageSlot(9, offset = 1020, size = 300))
        assertEquals(9, PdfZoom.pageAtCenter(pages, center = 500, atEnd = true, lastIndex = 9, fallback = 8))
    }
}

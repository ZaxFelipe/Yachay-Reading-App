package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {

    @Test
    fun `normalize drops accents and case`() {
        assertEquals("acao", TextSearch.normalize("Ação"))
        assertEquals("coracao pinguim", TextSearch.normalize("CORAÇÃO Pingüim"))
    }

    @Test
    fun `matches point at the original text`() {
        val text = "A AÇÃO e a ação."
        val ranges = TextSearch.matches(text, "acao")
        assertEquals(listOf(2..5, 11..14), ranges)
        assertEquals("AÇÃO", text.substring(ranges[0]))
        // Texto já decomposto (acento como caractere separado) também aponta certo.
        val decomposed = "Acão"
        assertEquals(listOf(0..4), TextSearch.matches(decomposed, "acao"))
    }

    @Test
    fun `snippet cuts at words and adds ellipsis`() {
        val text = "Era uma vez um menino chamado Bentinho que morava na rua de Matacavalos com a família."
        val match = TextSearch.matches(text, "bentinho").single()
        val (snippet, range) = TextSearch.snippet(text, match, radius = 15)
        assertTrue(snippet, snippet.startsWith("…"))
        assertTrue(snippet, snippet.endsWith("…"))
        assertEquals("Bentinho", snippet.substring(range.first, range.last + 1))
        // Não corta palavra no meio.
        assertEquals("…chamado Bentinho que morava na…", snippet)
    }

    @Test
    fun `snippet without cuts has no ellipsis and collapses line breaks`() {
        val text = "Capitu\n\nolhos de ressaca"
        val match = TextSearch.matches(text, "olhos").single()
        val (snippet, range) = TextSearch.snippet(text, match)
        assertEquals("Capitu olhos de ressaca", snippet)
        assertEquals("olhos", snippet.substring(range.first, range.last + 1))
    }
}

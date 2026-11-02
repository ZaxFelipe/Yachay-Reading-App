package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicPagesTest {

    @Test
    fun `numbers compare as numbers`() {
        val names = listOf("10.jpg", "2.jpg", "1.jpg", "page10.png", "page2.png", "Page1.png")
        assertEquals(
            listOf("1.jpg", "2.jpg", "10.jpg", "Page1.png", "page2.png", "page10.png"),
            names.sortedWith(ComicPages.NaturalOrder),
        )
    }

    @Test
    fun `zero padding, case and names without numbers`() {
        assertEquals(listOf("p001", "p2", "p010"), listOf("p010", "p2", "p001").sortedWith(ComicPages.NaturalOrder))
        assertEquals(listOf("a", "B", "c"), listOf("c", "B", "a").sortedWith(ComicPages.NaturalOrder))
        assertEquals(listOf("cap1/p2", "cap1/p10", "cap2/p1"), listOf("cap2/p1", "cap1/p10", "cap1/p2").sortedWith(ComicPages.NaturalOrder))
        // Números maiores que Long não estouram.
        assertTrue(ComicPages.NaturalOrder.compare("p99999999999999999999", "p100000000000000000000") < 0)
    }

    @Test
    fun `only images are pages`() {
        assertTrue(ComicPages.isPage("01.JPG"))
        assertTrue(ComicPages.isPage("vol/02.jpeg"))
        assertTrue(ComicPages.isPage("x.png"))
        assertTrue(ComicPages.isPage("x.webp"))
        assertTrue(ComicPages.isPage("x.gif"))
        assertFalse(ComicPages.isPage("ComicInfo.xml"))
        assertFalse(ComicPages.isPage("Thumbs.db"))
        assertFalse(ComicPages.isPage("__MACOSX/._01.jpg"))
        assertFalse(ComicPages.isPage(".hidden.png"))
        assertFalse(ComicPages.isPage("pasta/"))
    }

    @Test
    fun `pages filters and sorts the archive`() {
        val entries = listOf("ComicInfo.xml", "pagina10.png", "__MACOSX/._pagina1.png", "pagina2.png", "pagina1.png", "Thumbs.db")
        assertEquals(listOf("pagina1.png", "pagina2.png", "pagina10.png"), ComicPages.pages(entries))
    }
}

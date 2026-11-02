package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFormatTest {

    @Test
    fun `detects by mime type`() {
        assertEquals(BookFormat.PDF, BookFormat.detect("application/pdf", null))
        assertEquals(BookFormat.EPUB, BookFormat.detect("application/epub+zip", "whatever.bin"))
    }

    @Test
    fun `falls back to extension when mime is generic`() {
        assertEquals(BookFormat.EPUB, BookFormat.detect("application/octet-stream", "Dom Casmurro.EPUB"))
        assertEquals(BookFormat.PDF, BookFormat.detect(null, "artigo.final.pdf"))
    }

    @Test
    fun `returns null for unsupported files`() {
        assertNull(BookFormat.detect("text/plain", "notas.txt"))
        assertNull(BookFormat.detect(null, "sem_extensao"))
        assertNull(BookFormat.detect(null, null))
    }

    @Test
    fun `picker mime types cover every format`() {
        val mimes = BookFormat.pickerMimeTypes.toSet()
        BookFormat.entries.forEach { assertTrue(it.name, mimes.containsAll(it.mimeTypes)) }
        // CBZ costuma chegar como ZIP/binário genérico: precisa aparecer no seletor.
        assertTrue(mimes.contains("application/zip"))
    }

    @Test
    fun `detects cbz by mime or by extension`() {
        assertEquals(BookFormat.CBZ, BookFormat.detect("application/vnd.comicbook+zip", null))
        assertEquals(BookFormat.CBZ, BookFormat.detect("application/x-cbz", "x"))
        assertEquals(BookFormat.CBZ, BookFormat.detect("application/octet-stream", "Turma.CBZ"))
        assertEquals(BookFormat.CBZ, BookFormat.detect("application/zip", "mangá vol 1.cbz"))
        // ZIP qualquer não vira livro.
        assertNull(BookFormat.detect("application/zip", "fotos.zip"))
        assertTrue(BookFormat.CBZ.isPaged)
    }
}

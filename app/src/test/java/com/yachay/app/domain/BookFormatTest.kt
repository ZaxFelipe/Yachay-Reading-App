package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(setOf("application/pdf", "application/epub+zip"), BookFormat.pickerMimeTypes.toSet())
    }
}

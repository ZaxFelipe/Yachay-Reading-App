package com.yachay.app.ui.library

import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.BookFormat
import com.yachay.app.domain.LibrarySort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    private fun book(
        title: String,
        author: String? = null,
        addedAt: Long = 0,
        lastOpenedAt: Long? = null,
        progress: Float = 0f,
    ) = BookEntity(
        uri = "content://$title",
        title = title,
        author = author,
        format = BookFormat.EPUB,
        addedAt = addedAt,
        lastOpenedAt = lastOpenedAt,
        progress = progress,
    )

    private fun List<BookEntity>.titles() = map { it.title }

    @Test
    fun `title sort keeps accented words with their letter`() {
        val books = listOf(book("Zebra"), book("Árvore"), book("abelha"), book("Bola"))
        assertEquals(listOf("abelha", "Árvore", "Bola", "Zebra"), sortBooks(books, LibrarySort.TITLE).titles())
    }

    @Test
    fun `author sort puts books without author last and ties by title`() {
        val books = listOf(
            book("Sem autor"),
            book("Quincas Borba", "Machado de Assis"),
            book("A Hora da Estrela", "Clarice Lispector"),
            book("Dom Casmurro", "Machado de Assis"),
            book("Vazio", ""),
        )
        assertEquals(
            listOf("A Hora da Estrela", "Dom Casmurro", "Quincas Borba", "Sem autor", "Vazio"),
            sortBooks(books, LibrarySort.AUTHOR).titles(),
        )
    }

    @Test
    fun `added and opened sorts are most recent first`() {
        val books = listOf(
            book("A", addedAt = 1, lastOpenedAt = 50),
            book("B", addedAt = 3),
            book("C", addedAt = 2, lastOpenedAt = 90),
        )
        assertEquals(listOf("B", "C", "A"), sortBooks(books, LibrarySort.ADDED).titles())
        // Nunca abertos vão para o fim.
        assertEquals(listOf("C", "A", "B"), sortBooks(books, LibrarySort.OPENED).titles())
    }

    @Test
    fun `progress sort shows most read first and ties by title`() {
        val books = listOf(book("C", progress = 0.2f), book("B", progress = 1f), book("A", progress = 0.2f))
        assertEquals(listOf("B", "A", "C"), sortBooks(books, LibrarySort.PROGRESS).titles())
    }

    @Test
    fun `search matches title or author ignoring case and accents`() {
        val dom = book("Dom Casmurro", "Machado de Assis")
        assertTrue(matches(dom, "machado"))
        assertTrue(matches(dom, "CASMU"))
        assertTrue(matches(book("Ação e Reação"), "acao"))
        assertTrue(matches(book("Coração"), "  coraçao "))
        assertFalse(matches(dom, "clarice"))
        assertFalse(matches(book("Sem autor"), "machado"))
        assertTrue(matches(dom, ""))
    }
}

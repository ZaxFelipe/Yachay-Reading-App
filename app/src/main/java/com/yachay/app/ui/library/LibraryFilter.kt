package com.yachay.app.ui.library

import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.LibrarySort
import com.yachay.app.domain.TextSearch
import java.text.Collator
import java.util.Locale

/** Ordem pt-BR ciente de acentos: "Árvore" fica junto dos "A", não depois do "Z". */
private val collator: Collator = Collator.getInstance(Locale("pt", "BR"))

private val byTitle = Comparator<BookEntity> { a, b -> collator.compare(a.title, b.title) }

/** Listas pequenas: ordena em memória, sem query nova no DAO. Empates caem no título. */
fun sortBooks(books: List<BookEntity>, sort: LibrarySort): List<BookEntity> = when (sort) {
    LibrarySort.ADDED -> books.sortedByDescending { it.addedAt }
    LibrarySort.OPENED -> books.sortedWith(
        compareBy<BookEntity> { it.lastOpenedAt == null }
            .thenByDescending { it.lastOpenedAt }
            .thenByDescending { it.addedAt },
    )
    LibrarySort.TITLE -> books.sortedWith(byTitle)
    LibrarySort.AUTHOR -> books.sortedWith(
        // Sem autor vai para o fim.
        compareBy<BookEntity> { it.author.isNullOrBlank() }
            .then { a, b -> collator.compare(a.author.orEmpty(), b.author.orEmpty()) }
            .then(byTitle),
    )
    LibrarySort.PROGRESS -> books.sortedWith(compareByDescending<BookEntity> { it.progress }.then(byTitle))
}

/** Título ou autor contém o termo, sem diferenciar maiúsculas e acentos. Termo vazio = tudo. */
fun matches(book: BookEntity, query: String): Boolean =
    query.isBlank() || TextSearch.contains(book.title, query) || TextSearch.contains(book.author, query)

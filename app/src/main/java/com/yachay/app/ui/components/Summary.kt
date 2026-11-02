package com.yachay.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.yachay.app.R
import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.ReadingProgress

/**
 * "12 livros · 3 em andamento · 2 lidos", calculado das linhas exibidas. Partes zeradas somem
 * (exceto o total), e o zero tem texto próprio porque o plural pt-BR do Android trata 0 como "one".
 */
@Composable
fun booksSummary(books: List<BookEntity>): String {
    if (books.isEmpty()) return stringResource(R.string.summary_no_books)
    val reading = books.count { it.isReading }
    val done = books.count { ReadingProgress.isDone(it.progress) }
    return buildList {
        add(pluralStringResource(R.plurals.summary_books, books.size, books.size))
        if (reading > 0) add(stringResource(R.string.summary_reading, reading))
        if (done > 0) add(pluralStringResource(R.plurals.summary_done, done, done))
    }.joinToString(" · ")
}

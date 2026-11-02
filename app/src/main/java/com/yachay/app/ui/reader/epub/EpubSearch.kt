package com.yachay.app.ui.reader.epub

import android.content.Context
import com.yachay.app.domain.TextSearch
import com.yachay.app.ui.reader.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.publication.services.search.search

/**
 * Busca do Readium (`StringSearchService`, que o parser de EPUB já instala): sem diferenciar
 * maiúsculas nem acentos. Cada página de resultados do iterador vira um lote.
 */
@OptIn(ExperimentalReadiumApi::class)
fun epubSearchEngine(
    context: Context,
    publication: Publication,
    annotations: EpubAnnotations,
): (String) -> Flow<List<SearchHit<Locator>>> = { query ->
    flow {
        val options = SearchService.Options(caseSensitive = false, diacriticSensitive = false)
        val iterator = publication.search(query, options) ?: return@flow
        try {
            while (true) {
                val page = iterator.next().getOrNull() ?: break
                emit(page.locators.map { hit(context, annotations, it) })
            }
        } finally {
            iterator.close()
        }
    }.flowOn(Dispatchers.IO)
}

private fun hit(context: Context, annotations: EpubAnnotations, locator: Locator): SearchHit<Locator> {
    val before = locator.text.before.orEmpty()
    val term = locator.text.highlight.orEmpty()
    val full = before + term + locator.text.after.orEmpty()
    val (snippet, match) = if (term.isEmpty()) {
        full to IntRange.EMPTY
    } else {
        TextSearch.snippet(full, before.length until before.length + term.length)
    }
    return SearchHit(annotations.label(context, locator), snippet, match, locator)
}

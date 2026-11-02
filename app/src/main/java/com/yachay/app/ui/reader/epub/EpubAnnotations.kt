package com.yachay.app.ui.reader.epub

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.yachay.app.R
import com.yachay.app.data.db.AnnotationEntity
import com.yachay.app.data.db.AnnotationType
import com.yachay.app.data.repo.AnnotationRepository
import com.yachay.app.domain.Bookmarks
import com.yachay.app.domain.EpubSpot
import com.yachay.app.domain.ReadingProgress
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.AnnotationItem
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Purple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Marcadores e destaques de um EPUB: lê as anotações salvas (posição = `Locator.toJSON()`),
 * monta os rótulos da lista e as decorações dos destaques.
 */
class EpubAnnotations(
    private val bookId: Long,
    private val publication: Publication,
    private val repository: AnnotationRepository,
) {
    class Entry(val annotation: AnnotationEntity, val locator: Locator) {
        val spot: EpubSpot = spotOf(locator)
    }

    /** Anotações com o locator já lido (posições corrompidas são ignoradas), em ordem de leitura. */
    val entries: Flow<List<Entry>> = repository.observe(bookId).map { list ->
        list.mapNotNull { annotation ->
            runCatching { Locator.fromJSON(JSONObject(annotation.position)) }.getOrNull()?.let { Entry(annotation, it) }
        }.sortedWith(compareBy(Bookmarks.EpubOrder) { it.spot })
    }

    /** Marcador da tela atual, se houver. */
    fun bookmarkAt(entries: List<Entry>, locator: Locator?): Entry? {
        if (locator == null || locator.locations.progression == null) return null
        val here = spotOf(locator)
        return entries.firstOrNull { it.annotation.type == AnnotationType.BOOKMARK && Bookmarks.sameScreen(it.spot, here) }
    }

    suspend fun addBookmark(locator: Locator) = repository.addBookmark(bookId, locator.toJSON().toString())

    suspend fun addHighlight(locator: Locator, text: String) = repository.addHighlight(bookId, locator.toJSON().toString(), text)

    suspend fun remove(id: Long) = repository.remove(id)

    /** "Capítulo 3 · 40%": título do sumário (ou o número do capítulo) e a posição no livro. */
    fun label(context: Context, locator: Locator): String {
        val chapter = chapterTitle(locator)
            ?: context.getString(R.string.annotation_chapter_number, chapterNumber(locator))
        val percent = ReadingProgress.percent(ReadingProgress.forEpub(locator.locations.totalProgression))
        return context.getString(R.string.annotation_chapter_percent, chapter, percent)
    }

    fun items(context: Context, entries: List<Entry>, type: AnnotationType): List<AnnotationItem> =
        entries.filter { it.annotation.type == type }.map {
            AnnotationItem(it.annotation.id, label(context, it.locator), it.annotation.text)
        }

    /** Destaque translúcido: limão no Papel, roxo no Noturno (o Readium aplica a transparência). */
    fun decorations(entries: List<Entry>, theme: ReadingTheme): List<Decoration> {
        val tint = (if (theme == ReadingTheme.NIGHT) Purple else Lime).toArgb()
        return entries.filter { it.annotation.type == AnnotationType.HIGHLIGHT }.map {
            Decoration(id = decorationId(it.annotation.id), locator = it.locator, style = Decoration.Style.Highlight(tint = tint))
        }
    }

    private fun chapterTitle(locator: Locator): String? {
        val href = locator.href.removeFragment().toString()
        fun find(links: List<Link>): String? = links.firstNotNullOfOrNull { link ->
            link.title?.takeIf { link.url().removeFragment().toString() == href && it.isNotBlank() } ?: find(link.children)
        }
        return find(publication.tableOfContents) ?: locator.title?.takeIf { it.isNotBlank() }
    }

    private fun chapterNumber(locator: Locator): Int {
        val href = locator.href.removeFragment().toString()
        val index = publication.readingOrder.indexOfFirst { it.url().removeFragment().toString() == href }
        return if (index >= 0) index + 1 else 1
    }

    companion object {
        const val HIGHLIGHTS_GROUP = "highlights"

        fun decorationId(annotationId: Long): String = "highlight-$annotationId"

        fun annotationId(decorationId: String): Long? = decorationId.removePrefix("highlight-").toLongOrNull()

        fun spotOf(locator: Locator): EpubSpot = EpubSpot(
            href = locator.href.removeFragment().toString(),
            progression = locator.locations.progression ?: 0.0,
            totalProgression = locator.locations.totalProgression,
        )
    }
}

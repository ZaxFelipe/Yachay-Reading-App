package com.yachay.app.data.importer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.yachay.app.data.db.BookDao
import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.BookFormat
import com.yachay.app.ui.reader.cbz.CbzDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.services.cover

/** Resultado agregado de uma importação em lote, usado para a mensagem na UI. */
data class ImportResult(val added: Int, val duplicates: Int, val failed: Int)

/**
 * Importa arquivos escolhidos no seletor do sistema (SAF): persiste a permissão de leitura,
 * detecta o formato, extrai título/autor/capa e grava no banco.
 */
class BookImporter(
    private val context: Context,
    private val dao: BookDao,
    private val coverStore: CoverStore,
    private val readium: ReadiumServices,
) {
    private val resolver = context.contentResolver

    private enum class Outcome { ADDED, DUPLICATE, FAILED }

    private class Extracted(
        val title: String,
        val author: String?,
        val totalPages: Int?,
        val cover: Bitmap?,
    )

    suspend fun import(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        val outcomes = uris.map { uri -> runCatching { importOne(uri) }.getOrDefault(Outcome.FAILED) }
        ImportResult(
            added = outcomes.count { it == Outcome.ADDED },
            duplicates = outcomes.count { it == Outcome.DUPLICATE },
            failed = outcomes.count { it == Outcome.FAILED },
        )
    }

    private suspend fun importOne(uri: Uri): Outcome {
        if (dao.getByUri(uri.toString()) != null) return Outcome.DUPLICATE

        val fileName = displayName(uri)
        val format = BookFormat.detect(resolver.getType(uri), fileName) ?: return Outcome.FAILED

        // Sem isso a URI deixa de ser legível depois que o app reinicia.
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val fallbackTitle = fileName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Sem título"
        val extracted = when (format) {
            BookFormat.PDF -> extractPdf(uri, fallbackTitle)
            BookFormat.EPUB -> extractEpub(uri, fallbackTitle)
            BookFormat.CBZ -> extractCbz(uri, fallbackTitle)
        } ?: return Outcome.FAILED

        val id = dao.insert(
            BookEntity(
                uri = uri.toString(),
                title = extracted.title,
                author = extracted.author,
                format = format,
                totalPages = extracted.totalPages,
                addedAt = System.currentTimeMillis(),
            )
        )
        if (id == -1L) return Outcome.DUPLICATE

        extracted.cover?.let { bitmap ->
            runCatching { dao.updateCover(id, coverStore.save(id, bitmap)) }
            bitmap.recycle()
        }
        return Outcome.ADDED
    }

    private fun extractPdf(uri: Uri, fallbackTitle: String): Extracted? {
        val pfd = resolver.openFileDescriptor(uri, "r") ?: return null
        return pfd.use {
            PdfRenderer(it).use { renderer ->
                if (renderer.pageCount == 0) return null
                val cover = renderer.openPage(0).use { page ->
                    val width = COVER_RENDER_WIDTH
                    val height = width * page.height / page.width
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
                Extracted(fallbackTitle, author = null, totalPages = renderer.pageCount, cover = cover)
            }
        }
    }

    private suspend fun extractEpub(uri: Uri, fallbackTitle: String): Extracted? {
        val publication = readium.open(uri) ?: return null
        return try {
            Extracted(
                title = publication.metadata.title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
                author = publication.metadata.authors.firstOrNull()?.name,
                totalPages = null,
                cover = publication.cover(),
            )
        } finally {
            publication.close()
        }
    }

    /** Capa = primeira imagem; arquivo sem imagens (ou que não é ZIP) falha a importação. */
    private suspend fun extractCbz(uri: Uri, fallbackTitle: String): Extracted? {
        val document = CbzDocument.open(readium, uri) ?: return null
        return try {
            Extracted(fallbackTitle, author = null, totalPages = document.pageCount, cover = CbzDocument.cover(document, COVER_RENDER_WIDTH))
        } finally {
            document.close()
        }
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment

    private companion object {
        const val COVER_RENDER_WIDTH = 480
    }
}

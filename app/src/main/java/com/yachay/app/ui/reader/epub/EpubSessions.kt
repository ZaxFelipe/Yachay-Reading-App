package com.yachay.app.ui.reader.epub

import android.net.Uri
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.importer.ReadiumServices
import com.yachay.app.data.settings.SettingsRepository
import com.yachay.app.domain.ReaderPreferences
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.util.concurrent.ConcurrentHashMap

/**
 * Publicações EPUB abertas, por id de livro.
 *
 * O `EpubNavigatorFragment` do Readium precisa da [Publication] já aberta no momento em que a
 * activity instala a FragmentFactory (antes de `super.onCreate`). Abrir é suspenso, então quem
 * chama abre aqui primeiro e só depois inicia a [EpubReaderActivity].
 */
class EpubSessions(
    private val readium: ReadiumServices,
    private val settings: SettingsRepository,
) {

    class Session(
        val bookId: Long,
        val title: String,
        val publication: Publication,
        val initialLocator: Locator?,
        /** Tema/fonte lidos ao abrir: o navegador precisa deles antes de a activity existir. */
        val initialPreferences: ReaderPreferences,
    )

    private val sessions = ConcurrentHashMap<Long, Session>()

    suspend fun open(book: BookEntity): Boolean {
        close(book.id)
        val publication = readium.open(Uri.parse(book.uri)) ?: return false
        val locator = book.lastPosition?.let { json ->
            runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
        }
        sessions[book.id] = Session(book.id, book.title, publication, locator, settings.currentReaderPreferences())
        return true
    }

    operator fun get(bookId: Long): Session? = sessions[bookId]

    fun close(bookId: Long) {
        sessions.remove(bookId)?.publication?.close()
    }
}

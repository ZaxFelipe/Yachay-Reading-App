package com.yachay.app.data.repo

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.yachay.app.data.db.BookDao
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.importer.CoverStore
import com.yachay.app.domain.ReadingProgress
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val dao: BookDao,
    private val coverStore: CoverStore,
    private val contentResolver: ContentResolver,
) {
    fun observeAll(): Flow<List<BookEntity>> = dao.observeAll()

    fun observeInCollection(collectionId: Long): Flow<List<BookEntity>> =
        dao.observeInCollection(collectionId)

    fun observeInProgress(limit: Int = 10): Flow<List<BookEntity>> =
        dao.observeInProgress(ReadingProgress.DONE_THRESHOLD, limit)

    fun observeRecentlyOpened(limit: Int = 5): Flow<List<BookEntity>> = dao.observeRecentlyOpened(limit)

    fun observe(id: Long): Flow<BookEntity?> = dao.observeById(id)

    suspend fun get(id: Long): BookEntity? = dao.getById(id)

    /** Grava a posição atual; o progresso só sobe (ver [ReadingProgress.merge]). */
    suspend fun saveProgress(id: Long, progress: Float, position: String?) =
        dao.updateProgress(id, progress.coerceIn(0f, 1f), position)

    /** Para livros terminados fora do app. */
    suspend fun markRead(id: Long) = dao.markRead(id)

    /** Único jeito de diminuir o progresso. */
    suspend fun markUnread(id: Long) = dao.markUnread(id)

    suspend fun markOpened(id: Long) = dao.markOpened(id, System.currentTimeMillis())

    /** Remove o livro, a capa em cache e devolve a permissão do SAF. Não apaga o arquivo do usuário. */
    suspend fun delete(book: BookEntity) {
        dao.delete(book.id)
        coverStore.delete(book.id)
        runCatching {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(book.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}

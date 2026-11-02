package com.yachay.app.data.repo

import com.yachay.app.data.db.AnnotationDao
import com.yachay.app.data.db.AnnotationEntity
import com.yachay.app.data.db.AnnotationType
import kotlinx.coroutines.flow.Flow

/** Marcadores e destaques de um livro. Somem junto com o livro (FK CASCADE). */
class AnnotationRepository(private val dao: AnnotationDao) {

    fun observe(bookId: Long): Flow<List<AnnotationEntity>> = dao.observeForBook(bookId)

    suspend fun addBookmark(bookId: Long, position: String): Long =
        dao.insert(AnnotationEntity(bookId = bookId, type = AnnotationType.BOOKMARK, position = position, createdAt = System.currentTimeMillis()))

    suspend fun addHighlight(bookId: Long, position: String, text: String): Long =
        dao.insert(
            AnnotationEntity(
                bookId = bookId,
                type = AnnotationType.HIGHLIGHT,
                position = position,
                text = text,
                createdAt = System.currentTimeMillis(),
            ),
        )

    suspend fun remove(id: Long) = dao.delete(id)
}

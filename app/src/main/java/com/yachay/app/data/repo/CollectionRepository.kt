package com.yachay.app.data.repo

import com.yachay.app.data.db.BookCollectionCrossRef
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.db.CollectionDao
import com.yachay.app.data.db.CollectionEntity
import com.yachay.app.data.db.CollectionWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepository(private val dao: CollectionDao) {

    fun observeWithCounts(): Flow<List<CollectionWithCount>> = dao.observeWithCounts()

    fun observe(id: Long): Flow<CollectionEntity?> = dao.observeById(id)

    /** collectionId → livros, recém-lidos primeiro. */
    fun observeBooksByCollection(): Flow<Map<Long, List<BookEntity>>> =
        dao.observeBooksByCollection().map { rows ->
            rows.groupBy({ it.collectionId }, { it.book })
        }

    fun observeCollectionIdsForBook(bookId: Long): Flow<List<Long>> =
        dao.observeCollectionIdsForBook(bookId)

    suspend fun create(name: String): Long =
        dao.insert(CollectionEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun setBookInCollection(bookId: Long, collectionId: Long, member: Boolean) {
        if (member) {
            dao.addBook(BookCollectionCrossRef(bookId, collectionId))
        } else {
            dao.removeBook(bookId, collectionId)
        }
    }
}

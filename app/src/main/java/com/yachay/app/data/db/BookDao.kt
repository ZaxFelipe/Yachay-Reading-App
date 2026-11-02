package com.yachay.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query(
        """
        SELECT b.* FROM books b
        INNER JOIN book_collection bc ON b.id = bc.bookId
        WHERE bc.collectionId = :collectionId
        ORDER BY b.title COLLATE NOCASE
        """
    )
    fun observeInCollection(collectionId: Long): Flow<List<BookEntity>>

    /** Livros iniciados e não concluídos, do mais recente para o mais antigo. */
    @Query(
        """
        SELECT * FROM books
        WHERE lastOpenedAt IS NOT NULL AND progress < :doneThreshold
        ORDER BY lastOpenedAt DESC
        LIMIT :limit
        """
    )
    fun observeInProgress(doneThreshold: Float, limit: Int): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE uri = :uri")
    suspend fun getByUri(uri: String): BookEntity?

    /** Retorna -1 se a URI já existir. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Query("UPDATE books SET coverPath = :coverPath WHERE id = :id")
    suspend fun updateCover(id: Long, coverPath: String?)

    @Query("UPDATE books SET progress = :progress, lastPosition = :position WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float, position: String?)

    @Query("UPDATE books SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun markOpened(id: Long, timestamp: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)
}

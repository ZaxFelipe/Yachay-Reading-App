package com.yachay.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query(
        """
        SELECT c.*, COUNT(bc.bookId) AS bookCount FROM collections c
        LEFT JOIN book_collection bc ON c.id = bc.collectionId
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeWithCounts(): Flow<List<CollectionWithCount>>

    /** Livros de todas as coleções, para as capas em leque da lista de coleções. */
    @Query(
        """
        SELECT bc.collectionId, b.* FROM book_collection bc
        INNER JOIN books b ON b.id = bc.bookId
        ORDER BY b.lastOpenedAt DESC, b.title COLLATE NOCASE
        """
    )
    fun observeBooksByCollection(): Flow<List<CollectionBook>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeById(id: Long): Flow<CollectionEntity?>

    @Query("SELECT collectionId FROM book_collection WHERE bookId = :bookId")
    fun observeCollectionIdsForBook(bookId: Long): Flow<List<Long>>

    @Insert
    suspend fun insert(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBook(ref: BookCollectionCrossRef)

    @Query("DELETE FROM book_collection WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBook(bookId: Long, collectionId: Long)
}

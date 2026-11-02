package com.yachay.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {

    /** Ordem de criação; a lista na tela reordena pela posição (que depende do formato). */
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdAt, id")
    fun observeForBook(bookId: Long): Flow<List<AnnotationEntity>>

    @Insert
    suspend fun insert(annotation: AnnotationEntity): Long

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun delete(id: Long)
}

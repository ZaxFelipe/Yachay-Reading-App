package com.yachay.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yachay.app.domain.BookFormat

/**
 * Um livro importado. O arquivo continua onde o usuário o deixou; guardamos apenas a
 * URI do SAF (com permissão persistida) e os metadados extraídos na importação.
 */
@Entity(tableName = "books", indices = [Index(value = ["uri"], unique = true)])
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val author: String?,
    val format: BookFormat,
    /** Caminho absoluto da capa em `filesDir/covers`, ou null se não houver. */
    val coverPath: String? = null,
    /** Total de páginas (só PDF). */
    val totalPages: Int? = null,
    /** Fração lida, 0f..1f. Ver [com.yachay.app.domain.ReadingProgress]. */
    val progress: Float = 0f,
    /** PDF: índice da página (String). EPUB: `Locator.toJSON()` do Readium. */
    val lastPosition: String? = null,
    val addedAt: Long,
    val lastOpenedAt: Long? = null,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "book_collection",
    primaryKeys = ["bookId", "collectionId"],
    indices = [Index("collectionId")],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BookCollectionCrossRef(
    val bookId: Long,
    val collectionId: Long,
)

/** Um livro com a coleção a que pertence (um livro em duas coleções aparece duas vezes). */
data class CollectionBook(
    val collectionId: Long,
    @Embedded val book: BookEntity,
)

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    val bookCount: Int,
)

package com.yachay.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yachay.app.AppContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.db.CollectionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Uma coleção e seus livros (recém-lidos primeiro). */
data class CollectionShelf(val collection: CollectionEntity, val books: List<BookEntity>)

class CollectionsViewModel(container: AppContainer) : ViewModel() {

    private val collections = container.collectionRepository

    /** null enquanto carrega, para não piscar o estado vazio. */
    val items: StateFlow<List<CollectionShelf>?> = combine(
        collections.observeWithCounts(),
        collections.observeBooksByCollection(),
    ) { all, booksById ->
        all.map { CollectionShelf(it.collection, booksById[it.collection.id].orEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) collections.create(name)
    }
}

class CollectionDetailViewModel(
    private val collectionId: Long,
    container: AppContainer,
) : ViewModel() {

    private val collections = container.collectionRepository

    val collection: StateFlow<CollectionEntity?> = collections.observe(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val books: StateFlow<List<BookEntity>?> = container.bookRepository.observeInCollection(collectionId)
        .map<List<BookEntity>, List<BookEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) collections.rename(collectionId, name)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        collections.delete(collectionId)
        onDone()
    }
}

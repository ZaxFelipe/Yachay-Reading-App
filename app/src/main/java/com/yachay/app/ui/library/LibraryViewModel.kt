package com.yachay.app.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yachay.app.AppContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.db.CollectionWithCount
import com.yachay.app.data.importer.ImportResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loaded: Boolean = false,
    /** A biblioteca inteira, para o resumo do topo (independe do filtro). */
    val allBooks: List<BookEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val inProgress: List<BookEntity> = emptyList(),
    val collections: List<CollectionWithCount> = emptyList(),
    /** null = todos os livros. */
    val selectedCollectionId: Long? = null,
    val importing: Boolean = false,
) {
    val selectedCollection: CollectionWithCount?
        get() = collections.firstOrNull { it.collection.id == selectedCollectionId }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(container: AppContainer) : ViewModel() {

    private val books = container.bookRepository
    private val collections = container.collectionRepository
    private val importer = container.bookImporter

    private val selectedCollectionId = MutableStateFlow<Long?>(null)
    private val importing = MutableStateFlow(false)

    private val _importResults = Channel<ImportResult>(Channel.BUFFERED)
    val importResults: Flow<ImportResult> = _importResults.receiveAsFlow()

    /** (biblioteca inteira, livros visíveis com o filtro atual) */
    private val shelves = selectedCollectionId.flatMapLatest { id ->
        val all = books.observeAll()
        if (id == null) all.map { it to it } else combine(all, books.observeInCollection(id)) { a, v -> a to v }
    }

    val state = combine(
        shelves,
        books.observeInProgress(),
        collections.observeWithCounts(),
        selectedCollectionId,
        importing,
    ) { (all, visible), inProgress, allCollections, selectedId, isImporting ->
        // Se a coleção filtrada foi excluída, volta para "Todos".
        val validSelection = selectedId?.takeIf { id -> allCollections.any { it.collection.id == id } }
        if (selectedId != null && validSelection == null) selectedCollectionId.value = null
        LibraryUiState(
            loaded = true,
            allBooks = all,
            books = visible,
            inProgress = inProgress,
            collections = allCollections,
            selectedCollectionId = validSelection,
            importing = isImporting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectCollection(id: Long?) {
        selectedCollectionId.value = id
    }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            val result = try {
                importer.import(uris)
            } finally {
                importing.value = false
            }
            _importResults.send(result)
        }
    }
}

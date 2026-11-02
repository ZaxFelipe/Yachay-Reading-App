package com.yachay.app.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yachay.app.AppContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.db.CollectionWithCount
import com.yachay.app.data.importer.ImportResult
import com.yachay.app.domain.DailyGoalProgress
import com.yachay.app.domain.LibrarySort
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
    /** Meta do dia, mostrada como sub-barra no "Lendo agora". */
    val goal: DailyGoalProgress = DailyGoalProgress(0),
    /** Avatar do botão de perfil (o escolhido ou o do nível). */
    val avatar: Int = 1,
    /** Dias seguidos batendo a meta (no subtítulo a partir de 2). */
    val streak: Int = 0,
    val sort: LibrarySort = LibrarySort.Default,
    /** null = busca fechada; "" = aberta sem texto. */
    val query: String? = null,
) {
    val searching: Boolean get() = query != null

    val selectedCollection: CollectionWithCount?
        get() = collections.firstOrNull { it.collection.id == selectedCollectionId }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(container: AppContainer) : ViewModel() {

    private val books = container.bookRepository
    private val collections = container.collectionRepository
    private val importer = container.bookImporter
    private val dailyGoal = container.dailyGoal

    private val selectedCollectionId = MutableStateFlow<Long?>(null)
    private val importing = MutableStateFlow(false)
    private val query = MutableStateFlow<String?>(null)
    private val settings = container.settings

    private val _importResults = Channel<ImportResult>(Channel.BUFFERED)
    val importResults: Flow<ImportResult> = _importResults.receiveAsFlow()

    /** (biblioteca inteira, livros visíveis com o filtro atual) */
    private val shelves = selectedCollectionId.flatMapLatest { id ->
        val all = books.observeAll()
        if (id == null) all.map { it to it } else combine(all, books.observeInCollection(id)) { a, v -> a to v }
    }

    private val baseState = combine(
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
    }

    // combine só aceita 5 fluxos tipados: meta, nível, busca e ordem entram num segundo passo.
    // Busca e ordem valem sobre a estante já filtrada pela coleção (listas pequenas: em memória).
    val state = combine(baseState, dailyGoal.progress, dailyGoal.stats, query, settings.librarySort) { base, goal, stats, q, sort ->
        base.copy(
            books = sortBooks(base.books.filter { matches(it, q.orEmpty()) }, sort),
            goal = goal,
            avatar = stats.avatar,
            streak = stats.streak,
            sort = sort,
            query = q,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectCollection(id: Long?) {
        selectedCollectionId.value = id
    }

    fun openSearch() {
        if (query.value == null) query.value = ""
    }

    fun setQuery(text: String) {
        query.value = text
    }

    /** Fechar a busca limpa o texto. */
    fun closeSearch() {
        query.value = null
    }

    fun setSort(sort: LibrarySort) = viewModelScope.launch { settings.setLibrarySort(sort) }

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

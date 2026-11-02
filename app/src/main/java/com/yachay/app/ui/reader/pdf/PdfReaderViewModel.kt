package com.yachay.app.ui.reader.pdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yachay.app.AppContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.goals.PageRecorded
import android.os.Build
import com.yachay.app.R
import com.yachay.app.domain.BookFormat
import com.yachay.app.domain.TextSearch
import com.yachay.app.ui.reader.BookSearch
import com.yachay.app.ui.reader.SearchHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.yachay.app.domain.DailyGoal
import com.yachay.app.ui.reader.PageSource
import com.yachay.app.ui.reader.cbz.CbzDocument
import com.yachay.app.domain.DailyGoalProgress
import com.yachay.app.domain.ReadingProgress
import kotlinx.coroutines.delay
import com.yachay.app.data.db.AnnotationType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

sealed interface PdfReaderState {
    data object Loading : PdfReaderState
    data object Error : PdfReaderState
    /** [document] é um PDF ou um CBZ: o leitor de páginas serve aos dois. */
    data class Ready(val book: BookEntity, val document: PageSource, val initialPage: Int) : PdfReaderState
}

@OptIn(FlowPreview::class)
class PdfReaderViewModel(
    private val bookId: Long,
    private val context: Context,
    private val container: AppContainer,
) : ViewModel() {

    private val repository = container.bookRepository
    private val dailyGoal = container.dailyGoal

    /** Meta do dia, para a sub-barra no topo. */
    val goal: StateFlow<DailyGoalProgress> = dailyGoal.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyGoalProgress(0))

    private val _goalNotice = MutableStateFlow(false)

    /** true por alguns segundos quando a meta passa de 60% ou é batida. */
    val goalNotice: StateFlow<Boolean> = _goalNotice.asStateFlow()

    private val _lastNotice = MutableStateFlow<PageRecorded?>(null)

    /** Página que gerou o último aviso: nível/sequência (fica após sumir, para a animação de saída). */
    val lastNotice: StateFlow<PageRecorded?> = _lastNotice.asStateFlow()

    private val annotations = container.annotations

    /** Marcadores do livro: página → id (PDF/CBZ só têm marcador por página). */
    val bookmarks: StateFlow<Map<Int, Long>> = annotations.observe(bookId)
        .map { list ->
            list.filter { it.type == AnnotationType.BOOKMARK }
                .mapNotNull { a -> a.position.toIntOrNull()?.let { it to a.id } }
                .toMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _jumpTo = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Páginas para onde a lista deve pular (toque num marcador). */
    val jumpTo: SharedFlow<Int> = _jumpTo.asSharedFlow()

    /** Busca no livro: só PDF no Android 15+ (CBZ é imagem; abaixo da API 35 não há texto). */
    val search = BookSearch(viewModelScope) { query -> pdfSearch(query) }

    val canSearch: Boolean get() = PdfDocument.supportsSearch && document is PdfDocument

    private fun pdfSearch(query: String): Flow<List<SearchHit<Int>>> = flow {
        val pdf = document as? PdfDocument ?: return@flow
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return@flow
        for (index in 0 until pdf.pageCount) {
            val text = pdf.pageText(index)
            val label = context.getString(R.string.annotation_page, index + 1)
            val hits = TextSearch.matches(text, query).map { match ->
                val (snippet, range) = TextSearch.snippet(text, match)
                SearchHit(label, snippet, range, index)
            }
            if (hits.isNotEmpty()) emit(hits)
        }
    }

    private val _state = MutableStateFlow<PdfReaderState>(PdfReaderState.Loading)
    val state: StateFlow<PdfReaderState> = _state.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private var document: PageSource? = null

    init {
        viewModelScope.launch {
            val book = repository.get(bookId)
            val doc = book?.let {
                val uri = Uri.parse(it.uri)
                when (it.format) {
                    BookFormat.CBZ -> CbzDocument.open(container.readium, uri)
                    else -> PdfDocument.open(context, uri)
                }
            }
            if (book == null || doc == null || doc.pageCount == 0) {
                _state.value = PdfReaderState.Error
                return@launch
            }
            document = doc
            repository.markOpened(bookId)
            val initialPage = (book.lastPosition?.toIntOrNull() ?: 0).coerceIn(0, doc.pageCount - 1)
            _currentPage.value = initialPage
            _state.value = PdfReaderState.Ready(book, doc, initialPage)
        }
        viewModelScope.launch {
            // drop(1): não regrava a posição inicial só por abrir o livro.
            _currentPage.drop(1).debounce(SAVE_DEBOUNCE_MS).collect(::save)
        }
        viewModelScope.launch {
            // Página conta para a meta só depois de alguns segundos nela (folhear não conta).
            _currentPage.debounce(DailyGoal.PAGE_DWELL_MS).collect { page ->
                if (_state.value !is PdfReaderState.Ready) return@collect
                val recorded = dailyGoal.recordPage(bookId, DailyGoal.pdfPage(page))
                if (recorded.shouldNotify) showGoalNotice(recorded)
            }
        }
    }

    private fun showGoalNotice(recorded: PageRecorded) = viewModelScope.launch {
        _lastNotice.value = recorded
        _goalNotice.value = true
        delay(NOTICE_MS)
        _goalNotice.value = false
    }

    /** Marca/desmarca a página; appScope para gravar mesmo se o leitor fechar em seguida. */
    fun toggleBookmark(page: Int) {
        val existing = bookmarks.value[page]
        container.appScope.launch {
            if (existing != null) annotations.remove(existing) else annotations.addBookmark(bookId, page.toString())
        }
    }

    fun removeAnnotation(id: Long) {
        container.appScope.launch { annotations.remove(id) }
    }

    fun jumpTo(page: Int) {
        _jumpTo.tryEmit(page)
    }

    fun onPageVisible(index: Int) {
        _currentPage.value = index
    }

    fun progressFor(page: Int): Float = ReadingProgress.forPages(page, document?.pageCount ?: 0)

    private fun save(page: Int) {
        val pageCount = document?.pageCount ?: return
        // appScope: precisa concluir mesmo com o ViewModel sendo limpo.
        container.appScope.launch {
            repository.saveProgress(bookId, ReadingProgress.forPages(page, pageCount), page.toString())
        }
    }

    override fun onCleared() {
        if (_state.value is PdfReaderState.Ready) save(_currentPage.value)
        document?.let { doc -> container.appScope.launch { doc.close() } }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 400L
        const val NOTICE_MS = 3_000L
    }
}

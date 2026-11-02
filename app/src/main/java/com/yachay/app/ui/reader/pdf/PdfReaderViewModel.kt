package com.yachay.app.ui.reader.pdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yachay.app.AppContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.ReadingProgress
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
    data class Ready(val book: BookEntity, val document: PdfDocument, val initialPage: Int) : PdfReaderState
}

@OptIn(FlowPreview::class)
class PdfReaderViewModel(
    private val bookId: Long,
    private val context: Context,
    private val container: AppContainer,
) : ViewModel() {

    private val repository = container.bookRepository

    private val _state = MutableStateFlow<PdfReaderState>(PdfReaderState.Loading)
    val state: StateFlow<PdfReaderState> = _state.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private var document: PdfDocument? = null

    init {
        viewModelScope.launch {
            val book = repository.get(bookId)
            val doc = book?.let { PdfDocument.open(context, Uri.parse(it.uri)) }
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
    }

    fun onPageVisible(index: Int) {
        _currentPage.value = index
    }

    fun progressFor(page: Int): Float = ReadingProgress.forPdf(page, document?.pageCount ?: 0)

    private fun save(page: Int) {
        val pageCount = document?.pageCount ?: return
        // appScope: precisa concluir mesmo com o ViewModel sendo limpo.
        container.appScope.launch {
            repository.saveProgress(bookId, ReadingProgress.forPdf(page, pageCount), page.toString())
        }
    }

    override fun onCleared() {
        if (_state.value is PdfReaderState.Ready) save(_currentPage.value)
        document?.let { doc -> container.appScope.launch { doc.close() } }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 400L
    }
}

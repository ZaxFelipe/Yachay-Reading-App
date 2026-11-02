package com.yachay.app.ui.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Uma ocorrência: onde está ("Página 12"), o trecho e o intervalo do termo nele, e para onde ir. */
data class SearchHit<T>(val label: String, val snippet: String, val match: IntRange, val target: T)

data class SearchState<T>(
    val query: String = "",
    val hits: List<SearchHit<T>> = emptyList(),
    val searching: Boolean = false,
)

/**
 * Busca no livro aberto. O texto digitado passa por debounce; um termo novo cancela a busca
 * anterior (`flatMapLatest`), e os resultados chegam aos poucos, página por página do motor
 * ([engine] emite lotes), para um livro grande não travar a tela.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BookSearch<T>(scope: CoroutineScope, private val engine: (String) -> Flow<List<SearchHit<T>>>) {

    val query = MutableStateFlow("")

    val state: StateFlow<SearchState<T>> = query
        .debounce(DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { term ->
            if (term.length < MIN_LENGTH) {
                flow { emit(SearchState(term)) }
            } else {
                flow {
                    var hits = emptyList<SearchHit<T>>()
                    emit(SearchState(term, hits, searching = true))
                    engine(term).collect { batch ->
                        if (hits.size < MAX_HITS) {
                            hits = (hits + batch).take(MAX_HITS)
                            emit(SearchState(term, hits, searching = true))
                        }
                    }
                    emit(SearchState(term, hits, searching = false))
                }.catch { emit(SearchState(term, emptyList(), searching = false)) }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, SearchState())

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_LENGTH = 2
        /** Mais que isso não ajuda a achar nada e pesa na lista. */
        const val MAX_HITS = 500
    }
}

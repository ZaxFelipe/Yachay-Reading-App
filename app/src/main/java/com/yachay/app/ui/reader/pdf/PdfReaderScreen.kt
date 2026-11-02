package com.yachay.app.ui.reader.pdf

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.colors
import com.yachay.app.ui.reader.pdfPageFilter
import androidx.compose.runtime.remember
import com.yachay.app.ui.components.NeoButton
import com.yachay.app.ui.components.NeoEmptyState
import com.yachay.app.ui.components.NeoIconButton
import com.yachay.app.ui.components.NeoProgressBar
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.theme.Danger
import com.yachay.app.ui.theme.PercentTextStyle
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.PurpleDeep
import com.yachay.app.ui.theme.neoBorder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Fundo do leitor = fundo do app (Night): as páginas brancas se destacam; a cor fica só nas barras. */
private val ReaderBackground = Night
private const val MAX_RENDER_WIDTH_PX = 1600

@Composable
fun PdfReaderScreen(bookId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel(key = "pdf-$bookId") {
        PdfReaderViewModel(bookId, context.applicationContext, context.appContainer)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val readingTheme by context.appContainer.settings.readingTheme
        .collectAsStateWithLifecycle(initialValue = ReadingTheme.Default)

    Column(
        Modifier
            .fillMaxSize()
            .background(ReaderBackground),
    ) {
        val ready = state as? PdfReaderState.Ready
        NeoTopBar(
            title = ready?.book?.title.orEmpty(),
            navigationIcon = {
                NeoIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
            bottom = ready?.let {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeoProgressBar(progress = viewModel.progressFor(currentPage), modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.reader_page_of, currentPage + 1, it.document.pageCount),
                            style = PercentTextStyle,
                        )
                    }
                }
            },
        )

        when (val s = state) {
            PdfReaderState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.reader_loading), style = MaterialTheme.typography.titleMedium)
            }
            PdfReaderState.Error -> Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NeoEmptyState(title = stringResource(R.string.reader_error_title), body = stringResource(R.string.book_open_failed), color = Danger)
                NeoButton(onClick = onBack, color = Cream) { Text(stringResource(R.string.action_back)) }
            }
            is PdfReaderState.Ready -> PdfPages(s, readingTheme, viewModel::onPageVisible)
        }
    }
}

@Composable
private fun PdfPages(state: PdfReaderState.Ready, theme: ReadingTheme, onPageVisible: (Int) -> Unit) {
    val document = state.document
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.initialPage)

    LaunchedEffect(listState, document) {
        snapshotFlow {
            val info = listState.layoutInfo
            // Página atual = a que cruza o centro da tela (não a que só aparece uma faixa no topo).
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            when {
                info.visibleItemsInfo.isEmpty() -> -1
                // A última página pode ser curta demais para alcançar o centro.
                !listState.canScrollForward -> document.pageCount - 1
                else -> info.visibleItemsInfo
                    .firstOrNull { it.offset <= center && it.offset + it.size >= center }
                    ?.index ?: listState.firstVisibleItemIndex
            }
        }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect(onPageVisible)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(document.pageCount, key = { it }) { index ->
            PdfPage(document, index, theme)
        }
    }
}

@Composable
private fun PdfPage(document: PdfDocument, index: Int, theme: ReadingTheme) {
    val filter = remember(theme) { theme.pdfPageFilter() }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(document.pageAspectRatios[index])
            .background(theme.colors().page)
            // Roxo profundo: separa as páginas também no tema noturno (página da cor do fundo).
            .neoBorder(width = 1.5.dp, color = PurpleDeep),
    ) {
        val widthPx = constraints.maxWidth.coerceIn(1, MAX_RENDER_WIDTH_PX)
        val bitmap by produceState<Bitmap?>(null, document, index, widthPx) {
            value = document.render(index, widthPx)
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = filter,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

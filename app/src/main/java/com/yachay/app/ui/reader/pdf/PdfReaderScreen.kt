package com.yachay.app.ui.reader.pdf

import android.graphics.Bitmap
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.R
import com.yachay.app.ui.reader.SearchSheet
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.flow.Flow
import com.yachay.app.ui.reader.BookmarkButton
import com.yachay.app.ui.reader.AnnotationsSheet
import com.yachay.app.ui.reader.AnnotationItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.filled.List
import com.yachay.app.ui.reader.PageSource
import com.yachay.app.appContainer
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.colors
import com.yachay.app.ui.reader.pdfPageFilter
import com.yachay.app.ui.components.DailyGoalNotice
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Fundo do leitor = fundo do app (Night): as páginas brancas se destacam; a cor fica só nas barras. */
private val ReaderBackground = Night
private const val MAX_RENDER_WIDTH_PX = 1600
private const val RERENDER_DELAY_MS = 200L

@Composable
fun PdfReaderScreen(bookId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel(key = "pdf-$bookId") {
        PdfReaderViewModel(bookId, context.applicationContext, context.appContainer)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val goalNotice by viewModel.goalNotice.collectAsStateWithLifecycle()
    val lastNotice by viewModel.lastNotice.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    var showAnnotations by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.search.query.collectAsStateWithLifecycle()
    val searchState by viewModel.search.state.collectAsStateWithLifecycle()
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
            actions = {
                if (ready != null) {
                    // Sem texto (CBZ, ou PDF abaixo do Android 15) a lupa nem aparece.
                    if (viewModel.canSearch) {
                        NeoIconButton(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_open),
                            onClick = { showSearch = true },
                        )
                    }
                    BookmarkButton(marked = currentPage in bookmarks, onClick = { viewModel.toggleBookmark(currentPage) })
                    NeoIconButton(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.annotations_open),
                        onClick = { showAnnotations = true },
                    )
                }
            },
            bottom = ready?.let {
                {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Box(Modifier.weight(1f)) {
                            NeoProgressBar(progress = viewModel.progressFor(currentPage), goal = goal)
                            DailyGoalNotice(visible = goalNotice, progress = goal, unlockedLevel = lastNotice?.unlockedLevel, streakBonus = lastNotice?.streakBonus)
                        }
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
            is PdfReaderState.Ready -> PdfPages(s, readingTheme, viewModel::onPageVisible, viewModel.jumpTo)
        }
    }

    if (showSearch && state is PdfReaderState.Ready) {
        SearchSheet(
            query = searchQuery,
            onQueryChange = { viewModel.search.query.value = it },
            state = searchState,
            onOpen = { hit ->
                viewModel.jumpTo(hit.target)
                showSearch = false
            },
            onDismiss = { showSearch = false },
        )
    }

    if (showAnnotations && state is PdfReaderState.Ready) {
        AnnotationsSheet(
            bookmarks = bookmarks.entries.sortedBy { it.key }.map { (page, id) ->
                AnnotationItem(id, stringResource(R.string.annotation_page, page + 1))
            },
            highlights = null,
            onOpen = { id ->
                bookmarks.entries.firstOrNull { it.value == id }?.let { viewModel.jumpTo(it.key) }
                showAnnotations = false
            },
            onRemove = viewModel::removeAnnotation,
            onDismiss = { showAnnotations = false },
        )
    }
}

@Composable
private fun PdfPages(
    state: PdfReaderState.Ready,
    theme: ReadingTheme,
    onPageVisible: (Int) -> Unit,
    jumpTo: Flow<Int>,
) {
    val document = state.document
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.initialPage)
    // Zoom não é salvo: reabrir o livro começa em 1×.
    val zoom = remember(listState) { PdfZoomState(listState) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(jumpTo) {
        jumpTo.collect { listState.scrollToItem(it.coerceIn(0, document.pageCount - 1)) }
    }

    LaunchedEffect(listState, document) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@snapshotFlow -1
            val height = info.viewportSize.height
            // Página atual = a que cruza o centro da tela (não a que só aparece uma faixa no topo),
            // contando só a parte da lista que o zoom deixa visível.
            val center = PdfZoom.visibleCenter(info.viewportStartOffset, height, zoom.scale, zoom.offsetY)
            val windowAtBottom = zoom.offsetY <= PdfZoom.offsetLimits(zoom.scale, height.toFloat()).start + 1f
            PdfZoom.pageAtCenter(
                pages = info.visibleItemsInfo.map { PageSlot(it.index, it.offset, it.size) },
                center = center,
                // A última página pode ser curta demais para alcançar o centro.
                atEnd = !listState.canScrollForward && windowAtBottom,
                lastIndex = document.pageCount - 1,
                fallback = listState.firstVisibleItemIndex,
            )
        }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect(onPageVisible)
    }

    // Nitidez: ~200 ms depois do último gesto, re-renderiza na resolução do zoom atual.
    var renderScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(zoom) {
        snapshotFlow { zoom.scale }.collectLatest {
            delay(RERENDER_DELAY_MS)
            renderScale = it
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { zoom.viewport = it }
            .nestedScroll(zoom.nestedScroll)
            .pointerInput(zoom) { detectPinchAndPan(zoom) }
            .pointerInput(zoom) {
                detectTapGestures(onDoubleTap = { focus -> scope.launch { zoom.toggle(focus) } })
            },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // A lista inteira é escalada: a rolagem vertical (e o fling) continuam sendo dela, e
            // os toques chegam já nas coordenadas da página ampliada.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offsetX
                    translationY = zoom.offsetY
                },
        ) {
            items(document.pageCount, key = { it }) { index ->
                PdfPage(document, index, theme, renderScale)
            }
        }
    }
}

/**
 * Pinça (2+ dedos) e arrasto horizontal ampliado. Observa no passe Initial, antes da lista:
 * a pinça é consumida (a lista não rola junto); o arrasto de um dedo não é consumido, então a
 * lista continua rolando na vertical enquanto aqui só se move o eixo horizontal.
 */
private suspend fun PointerInputScope.detectPinchAndPan(zoom: PdfZoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                zoom.transform(event.calculateCentroid(useCurrent = true), event.calculatePan(), event.calculateZoom())
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            } else if (pressed.size == 1 && zoom.zoomed) {
                val change = pressed.first()
                zoom.panX(change.position.x - change.previousPosition.x)
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
private fun PdfPage(document: PageSource, index: Int, theme: ReadingTheme, renderScale: Float) {
    val filter = remember(theme) { theme.pdfPageFilter() }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(document.pageAspectRatios[index])
            .background(theme.colors().page)
            // Roxo profundo: separa as páginas também no tema noturno (página da cor do fundo).
            .neoBorder(width = 1.5.dp, color = PurpleDeep),
    ) {
        val baseWidth = constraints.maxWidth.coerceIn(1, MAX_RENDER_WIDTH_PX)
        val widthPx = PdfZoom.renderWidthFor(
            viewportPx = baseWidth,
            scale = renderScale.coerceAtMost(PdfZoom.MAX_RENDER_SCALE),
            maxPx = document.maxRenderWidth(index),
        )
        // Mantém o bitmap anterior enquanto o novo (de outra resolução) renderiza: sem piscar.
        var bitmap by remember(document, index) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(document, index, widthPx) {
            document.render(index, widthPx)?.let { bitmap = it }
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

/**
 * Escala e deslocamento da lista inteira (`tela = escala × local + deslocamento`). O que a
 * página ampliada não consegue absorver na vertical vira rolagem da lista, para o arrasto
 * seguir de uma página para a outra sem ficar preso.
 */
@Stable
private class PdfZoomState(private val listState: LazyListState) {
    var scale by mutableFloatStateOf(PdfZoom.MIN)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set
    var viewport by mutableStateOf(IntSize.Zero)

    val zoomed: Boolean get() = scale > PdfZoom.MIN + 0.001f

    private fun limitsX() = PdfZoom.offsetLimits(scale, viewport.width.toFloat())
    private fun limitsY() = PdfZoom.offsetLimits(scale, viewport.height.toFloat())

    /** Zoom em torno de [focus] + [pan] (px de tela). */
    fun transform(focus: Offset, pan: Offset, zoom: Float) {
        val newScale = PdfZoom.clampZoom(scale * zoom)
        val x = PdfZoom.offsetForZoom(offsetX, focus.x, scale, newScale) + pan.x
        val y = PdfZoom.offsetForZoom(offsetY, focus.y, scale, newScale) + pan.y
        scale = newScale
        offsetX = x.coerceIn(limitsX())
        offsetY = y.coerceIn(limitsY())
        // Sobra vertical → rolagem da lista (em px da lista, sem zoom).
        val overflow = y - offsetY
        if (overflow != 0f) listState.dispatchRawDelta(-overflow / newScale)
    }

    fun panX(dx: Float) {
        offsetX = (offsetX + dx).coerceIn(limitsX())
    }

    /** Duplo toque: 1× ↔ 2,5×, ancorado no ponto tocado. */
    suspend fun toggle(focus: Offset) {
        val target = if (zoomed) PdfZoom.MIN else PdfZoom.DOUBLE_TAP
        animate(scale, target) { value, _ -> transform(focus, Offset.Zero, value / scale) }
    }

    /** A lista chegou na borda (início/fim) e sobrou arrasto: move a janela ampliada. */
    val nestedScroll = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!zoomed || available.y == 0f) return Offset.Zero
            // `available` vem nas coordenadas da lista (sem zoom); o deslocamento é em px de tela.
            val before = offsetY
            offsetY = (offsetY + available.y * scale).coerceIn(limitsY())
            return Offset(0f, (offsetY - before) / scale)
        }
    }
}

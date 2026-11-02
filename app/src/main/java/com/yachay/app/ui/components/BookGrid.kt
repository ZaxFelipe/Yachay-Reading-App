package com.yachay.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.yachay.app.data.db.BookEntity
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.paperGrain

/** Grade adaptativa de livros usada pela Biblioteca e pelo detalhe de coleção. */
@Composable
fun BookGrid(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        // Grão de papel fixo atrás da estante (não rola junto com os livros).
        modifier = modifier.paperGrain(),
        content = content,
    )
}

fun LazyGridScope.fullWidthItem(key: Any? = null, content: @Composable LazyGridItemScope.() -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, content = content)
}

fun LazyGridScope.bookCards(
    books: List<BookEntity>,
    onOpen: (BookEntity) -> Unit,
    onLongPress: (BookEntity) -> Unit,
) {
    items(books, key = { it.id }) { book ->
        BookCard(book = book, onClick = { onOpen(book) }, onLongClick = { onLongPress(book) })
    }
}

/** Snackbar no estilo da casa: plaquinha creme com contorno e sombra dura (é aviso, não dado). */
@Composable
fun NeoSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data ->
        NeoSurface(color = Cream, modifier = Modifier.padding(16.dp)) {
            Text(
                data.visuals.message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }
}

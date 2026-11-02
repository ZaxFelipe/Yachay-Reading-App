package com.yachay.app.ui.reader

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.BookFormat
import com.yachay.app.ui.reader.epub.EpubReaderActivity
import kotlinx.coroutines.launch

/**
 * Devolve a ação "abrir livro", que despacha para o leitor certo de cada formato:
 * PDF → rota Compose [com.yachay.app.ui.navigation.PdfReaderRoute];
 * EPUB → abre a publicação no Readium e inicia a [EpubReaderActivity].
 */
@Composable
fun rememberBookOpener(onOpenPdf: (bookId: Long) -> Unit): (BookEntity) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openPdf by rememberUpdatedState(onOpenPdf)

    return remember(context, scope) {
        { book ->
            when (book.format) {
                BookFormat.PDF -> openPdf(book.id)
                BookFormat.EPUB -> scope.launch {
                    if (context.appContainer.epubSessions.open(book)) {
                        context.startActivity(EpubReaderActivity.intent(context, book.id))
                    } else {
                        Toast.makeText(context, R.string.book_open_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

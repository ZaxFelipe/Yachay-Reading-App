package com.yachay.app.ui.collections

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.ui.components.BookGrid
import com.yachay.app.ui.components.NeoDialog
import com.yachay.app.ui.components.NeoEmptyState
import com.yachay.app.ui.components.NeoIconButton
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.components.bookCards
import com.yachay.app.ui.components.fullWidthItem
import com.yachay.app.ui.reader.rememberBookOpener
import com.yachay.app.ui.theme.Danger
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.placeholderColorFor
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.neoBorder
import com.yachay.app.ui.components.TopBarSubtitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.yachay.app.ui.components.booksSummary

@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onBack: () -> Unit,
    onOpenPdf: (Long) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel(key = "collection-$collectionId") {
        CollectionDetailViewModel(collectionId, context.appContainer)
    }
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val openBook = rememberBookOpener(onOpenPdf)

    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        containerColor = Night,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            NeoTopBar(
                title = collection?.name.orEmpty(),
                navigationIcon = {
                    NeoIconButton(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                },
                actions = {
                    NeoIconButton(Icons.Filled.Edit, stringResource(R.string.action_rename), onClick = { renaming = true })
                    NeoIconButton(Icons.Filled.Delete, stringResource(R.string.action_delete), onClick = { confirmDelete = true })
                },
                // A cor da coleção aparece como um selo ao lado do resumo: a mesma do painel dela na lista.
                bottom = books?.let { list ->
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .background(placeholderColorFor(collectionId), NeoDefaults.Shape)
                                    .neoBorder(width = 2.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            TopBarSubtitle(booksSummary(list))
                        }
                    }
                },
            )
        },
    ) { padding ->
        BookGrid(Modifier.padding(padding)) {
            val list = books
            if (list != null && list.isEmpty()) {
                fullWidthItem(key = "empty") {
                    NeoEmptyState(
                        title = stringResource(R.string.library_collection_empty),
                        body = stringResource(R.string.collection_detail_empty),
                        color = Cream,
                    )
                }
            } else {
                bookCards(list.orEmpty(), onOpen = openBook, onLongPress = { actionsFor = it })
            }
        }
    }

    if (renaming) {
        CollectionNameDialog(
            title = stringResource(R.string.collections_rename_title),
            confirmText = stringResource(R.string.action_save),
            initial = collection?.name.orEmpty(),
            onConfirm = { name ->
                viewModel.rename(name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }

    if (confirmDelete) {
        NeoDialog(
            title = stringResource(R.string.collections_delete_title),
            confirmText = stringResource(R.string.action_delete),
            confirmColor = Danger,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDone = onBack)
            },
            onDismiss = { confirmDelete = false },
        ) {
            Text(
                stringResource(R.string.collections_delete_body, collection?.name.orEmpty()),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    actionsFor?.let { book ->
        BookActionsSheet(book = book, onDismiss = { actionsFor = null })
    }
}

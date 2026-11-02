package com.yachay.app.ui.library

import android.content.res.Resources
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.data.importer.ImportResult
import com.yachay.app.domain.BookFormat
import com.yachay.app.ui.collections.BookActionsSheet
import com.yachay.app.ui.profile.ProfileButton
import com.yachay.app.ui.components.AlsoReadingCard
import com.yachay.app.ui.components.BookGrid
import com.yachay.app.ui.components.NeoButton
import com.yachay.app.ui.components.NeoChip
import com.yachay.app.ui.components.NeoEmptyState
import com.yachay.app.ui.components.NeoFab
import com.yachay.app.ui.components.NeoSnackbarHost
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.components.ReadingNowCard
import com.yachay.app.ui.components.SectionHeader
import com.yachay.app.ui.components.Sparkle
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.components.TopBarSubtitle
import com.yachay.app.ui.components.bookCards
import com.yachay.app.ui.components.booksSummary
import com.yachay.app.ui.components.fullWidthItem
import com.yachay.app.ui.reader.rememberBookOpener
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Night

/** Pergunta da tela: "onde eu parei?". Responde com "Lendo agora" antes da estante. */
@Composable
fun LibraryScreen(onOpenPdf: (Long) -> Unit, onOpenProfile: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel { LibraryViewModel(context.appContainer) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openBook = rememberBookOpener(onOpenPdf)
    val snackbar = remember { SnackbarHostState() }
    var actionsFor by remember { mutableStateOf<BookEntity?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris)
    }
    val pickBooks = { picker.launch(BookFormat.pickerMimeTypes) }

    LaunchedEffect(viewModel) {
        viewModel.importResults.collect { result ->
            snackbar.showSnackbar(importMessage(context.resources, result))
        }
    }

    Scaffold(
        containerColor = Night,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { NeoSnackbarHost(snackbar) },
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.app_name),
                navigationIcon = { ProfileButton(avatar = state.avatar, onClick = onOpenProfile) },
                actions = {
                    Sparkle(Modifier.padding(end = 4.dp), extent = 24.dp, color = Lime, outline = Ink)
                },
                bottom = {
                    TopBarSubtitle(
                        if (state.importing) stringResource(R.string.library_importing) else booksSummary(state.allBooks),
                    )
                },
            )
        },
        floatingActionButton = {
            // Com a estante vazia a ação já está no estado vazio; não repetimos o botão.
            if (state.allBooks.isNotEmpty()) {
                NeoFab(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.library_add_books),
                    onClick = pickBooks,
                )
            }
        },
    ) { padding ->
        BookGrid(Modifier.padding(padding)) {
            if (state.collections.isNotEmpty()) {
                fullWidthItem(key = "filters") {
                    CollectionFilterRow(state = state, onSelect = viewModel::selectCollection)
                }
            }

            val showReading = state.selectedCollectionId == null && state.inProgress.isNotEmpty()
            if (showReading) {
                val (current, others) = state.inProgress.first() to state.inProgress.drop(1)
                // Título e conteúdo no mesmo item: ficam juntos (12dp) e longe da seção anterior.
                fullWidthItem(key = "reading-now") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(stringResource(R.string.library_reading_now))
                        ReadingNowCard(current, goal = state.goal, onContinue = { openBook(current) }, onLongClick = { actionsFor = current })
                    }
                }
                if (others.isNotEmpty()) {
                    fullWidthItem(key = "also-reading") {
                        Column(
                            Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SectionHeader(stringResource(R.string.library_also_reading), count = others.size)
                            if (others.size == 1) {
                                // Um só: ocupa a largura toda em vez de deixar meia tela vazia.
                                val book = others.single()
                                AlsoReadingCard(
                                    book,
                                    onClick = { openBook(book) },
                                    onLongClick = { actionsFor = book },
                                    modifier = Modifier.fillMaxWidth(),
                                    fill = true,
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(end = 4.dp),
                                ) {
                                    items(others, key = { it.id }) { book ->
                                        AlsoReadingCard(book, onClick = { openBook(book) }, onLongClick = { actionsFor = book })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.books.isNotEmpty()) {
                fullWidthItem(key = "books-header") {
                    SectionHeader(
                        modifier = Modifier.padding(top = if (showReading) 8.dp else 0.dp),
                        title = state.selectedCollection?.collection?.name ?: stringResource(R.string.library_all_books),
                        count = state.books.size,
                    )
                }
                bookCards(state.books, onOpen = openBook, onLongPress = { actionsFor = it })
            } else if (state.loaded) {
                fullWidthItem(key = "empty") {
                    if (state.selectedCollectionId == null) {
                        NeoEmptyState(
                            title = stringResource(R.string.library_empty_title),
                            body = stringResource(R.string.library_empty_body),
                            action = {
                                NeoButton(onClick = pickBooks, color = Lime) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Text(stringResource(R.string.library_add_books))
                                }
                            },
                        )
                    } else {
                        NeoEmptyState(
                            title = state.selectedCollection?.collection?.name.orEmpty(),
                            body = stringResource(R.string.library_collection_empty),
                            color = Cream,
                        )
                    }
                }
            }
        }
    }

    actionsFor?.let { book ->
        BookActionsSheet(book = book, onDismiss = { actionsFor = null })
    }
}

@Composable
private fun CollectionFilterRow(state: LibraryUiState, onSelect: (Long?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        item {
            NeoChip(
                label = stringResource(R.string.library_filter_all),
                selected = state.selectedCollectionId == null,
                onClick = { onSelect(null) },
            )
        }
        items(state.collections, key = { it.collection.id }) { item ->
            NeoChip(
                label = item.collection.name,
                selected = state.selectedCollectionId == item.collection.id,
                onClick = { onSelect(item.collection.id) },
            )
        }
    }
}

private fun importMessage(resources: Resources, result: ImportResult): String {
    val parts = buildList {
        if (result.added > 0) add(resources.getQuantityString(R.plurals.import_added, result.added, result.added))
        if (result.duplicates > 0) add(resources.getQuantityString(R.plurals.import_duplicates, result.duplicates, result.duplicates))
        if (result.failed > 0) add(resources.getQuantityString(R.plurals.import_failed, result.failed, result.failed))
    }
    return parts.joinToString(" · ").ifEmpty { resources.getString(R.string.import_nothing) }
}

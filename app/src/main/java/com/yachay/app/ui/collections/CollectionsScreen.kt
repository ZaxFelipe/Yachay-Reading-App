package com.yachay.app.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.ui.components.CoverFan
import com.yachay.app.ui.components.NeoButton
import com.yachay.app.ui.components.NeoDialog
import com.yachay.app.ui.components.booksSummary
import com.yachay.app.ui.components.NeoEmptyState
import com.yachay.app.ui.components.NeoFab
import com.yachay.app.ui.components.NeoPressable
import com.yachay.app.ui.components.NeoTextField
import com.yachay.app.ui.components.NeoTopBar
import com.yachay.app.ui.components.TopBarSubtitle
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.Halftone
import com.yachay.app.ui.theme.halftone
import com.yachay.app.ui.theme.paperGrain
import com.yachay.app.ui.theme.shadeFor
import com.yachay.app.ui.theme.Lilac
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Night
import com.yachay.app.ui.theme.placeholderColorFor

@Composable
fun CollectionsScreen(onOpenCollection: (Long) -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel { CollectionsViewModel(context.appContainer) }
    val items by viewModel.items.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Night,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.nav_collections),
                bottom = items?.let { list ->
                    {
                        TopBarSubtitle(
                            if (list.isEmpty()) stringResource(R.string.collections_empty_title)
                            else pluralStringResource(R.plurals.collections_count, list.size, list.size),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            NeoFab(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.collections_new),
                onClick = { creating = true },
                color = Lime,
            )
        },
    ) { padding ->
        val list = items
        if (list != null && list.isEmpty()) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                NeoEmptyState(
                    title = stringResource(R.string.collections_empty_title),
                    body = stringResource(R.string.collections_empty_body),
                    color = Lilac,
                    action = {
                        NeoButton(onClick = { creating = true }, color = Cream) {
                            Text(stringResource(R.string.collections_new))
                        }
                    },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .paperGrain(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(list.orEmpty(), key = { it.collection.id }) { shelf ->
                    CollectionRow(shelf, onClick = { onOpenCollection(shelf.collection.id) })
                }
            }
        }
    }

    if (creating) {
        CollectionNameDialog(
            title = stringResource(R.string.collections_new),
            confirmText = stringResource(R.string.action_create),
            initial = "",
            onConfirm = { name ->
                viewModel.create(name)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}

/**
 * Linha de coleção: à esquerda, um painel na cor da coleção com as capas em leque (o que a
 * pessoa reconhece de relance); à direita, nome e o resumo de leitura dela.
 */
@Composable
private fun CollectionRow(shelf: CollectionShelf, onClick: () -> Unit) {
    val hue = placeholderColorFor(shelf.collection.id)
    NeoPressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(104.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .background(hue)
                    .halftone(Halftone(shadeFor(hue), Alignment.TopStart, spacing = 8.dp, maxDot = 3.dp, reach = 0.9f))
                    .drawBehind {
                        val stroke = NeoDefaults.BorderWidth.toPx()
                        drawLine(Ink, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                CoverFan(shelf.books)
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    shelf.collection.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (shelf.books.isEmpty()) stringResource(R.string.collections_book_count_zero) else booksSummary(shelf.books),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

/** Diálogo reaproveitado para criar e renomear coleções. */
@Composable
fun CollectionNameDialog(
    title: String,
    confirmText: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    NeoDialog(
        title = title,
        confirmText = confirmText,
        confirmEnabled = name.isNotBlank(),
        onConfirm = { onConfirm(name) },
        onDismiss = onDismiss,
    ) {
        NeoTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.collections_name_hint),
            onDone = { if (name.isNotBlank()) onConfirm(name) },
        )
    }
}

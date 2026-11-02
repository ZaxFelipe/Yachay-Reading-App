package com.yachay.app.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yachay.app.R
import com.yachay.app.ui.components.NeoButton
import com.yachay.app.ui.components.NeoChip
import com.yachay.app.ui.components.NeoIconButton
import com.yachay.app.ui.components.NeoPressable
import com.yachay.app.ui.components.NeoSheet
import com.yachay.app.ui.components.YachayIcons
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Danger
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.Paper

/** Um marcador ou destaque já pronto para a lista: rótulo da posição e, no destaque, o trecho. */
data class AnnotationItem(val id: Long, val label: String, val text: String? = null)

/**
 * Lista de marcadores e destaques do livro, em duas abas. Toque vai até a posição; o ícone de
 * lixeira remove. [highlights] = null esconde a aba (PDF não tem destaques).
 */
@Composable
fun AnnotationsSheet(
    bookmarks: List<AnnotationItem>,
    highlights: List<AnnotationItem>?,
    onOpen: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var showHighlights by rememberSaveable { mutableStateOf(false) }
    val showingHighlights = showHighlights && highlights != null
    NeoSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (highlights != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeoChip(stringResource(R.string.annotations_bookmarks), selected = !showHighlights, onClick = { showHighlights = false })
                    NeoChip(stringResource(R.string.annotations_highlights), selected = showHighlights, onClick = { showHighlights = true })
                }
            } else {
                Text(stringResource(R.string.annotations_bookmarks), style = MaterialTheme.typography.titleLarge)
            }
            val items = if (showingHighlights) highlights.orEmpty() else bookmarks
            if (items.isEmpty()) {
                Text(
                    stringResource(if (showingHighlights) R.string.annotations_highlights_empty else R.string.annotations_bookmarks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                val removeLabel = stringResource(if (showingHighlights) R.string.annotation_remove_highlight else R.string.annotation_remove_bookmark)
                LazyColumn(
                    Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        AnnotationRow(item, removeLabel, onOpen = { onOpen(item.id) }, onRemove = { onRemove(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationRow(item: AnnotationItem, removeLabel: String, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NeoPressable(onClick = onOpen, modifier = Modifier.weight(1f), color = Paper, shadow = 3.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(item.label, style = MaterialTheme.typography.titleSmall)
                item.text?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = InkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        NeoIconButton(Icons.Filled.Delete, contentDescription = removeLabel, onClick = onRemove)
    }
}

/** Marcador da barra do leitor: preenchido quando a página atual já está marcada. */
@Composable
fun BookmarkButton(marked: Boolean, onClick: () -> Unit) {
    NeoIconButton(
        if (marked) YachayIcons.Bookmark else YachayIcons.BookmarkBorder,
        contentDescription = stringResource(if (marked) R.string.annotation_remove_bookmark else R.string.bookmark_add),
        onClick = onClick,
        color = if (marked) Lime else Cream,
    )
}

/** Tocar num destaque na página: confirma a remoção. */
@Composable
fun HighlightActionsSheet(text: String?, onRemove: () -> Unit, onDismiss: () -> Unit) {
    NeoSheet(onDismiss = onDismiss, dimBackground = false) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            text?.let {
                Text("“$it”", style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            NeoButton(onClick = onRemove, color = Danger, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text(stringResource(R.string.annotation_remove_highlight))
            }
        }
    }
}

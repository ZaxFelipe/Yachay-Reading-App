package com.yachay.app.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yachay.app.AppContainer
import com.yachay.app.R
import com.yachay.app.appContainer
import com.yachay.app.data.db.BookEntity
import com.yachay.app.ui.components.BookCover
import com.yachay.app.ui.components.BookStatus
import com.yachay.app.ui.components.NeoButton
import com.yachay.app.ui.components.NeoDialog
import com.yachay.app.ui.components.NeoTextField
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.Danger
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.neoBorder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionOption(val id: Long, val name: String, val checked: Boolean)

class BookActionsViewModel(
    private val bookId: Long,
    container: AppContainer,
) : ViewModel() {
    private val books = container.bookRepository
    private val collections = container.collectionRepository

    val options = combine(
        collections.observeWithCounts(),
        collections.observeCollectionIdsForBook(bookId),
    ) { all, memberIds ->
        all.map { CollectionOption(it.collection.id, it.collection.name, it.collection.id in memberIds) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(collectionId: Long, member: Boolean) = viewModelScope.launch {
        collections.setBookInCollection(bookId, collectionId, member)
    }

    fun createAndAdd(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val id = collections.create(name)
        collections.setBookInCollection(bookId, id, true)
    }

    fun delete(book: BookEntity, onDone: () -> Unit) = viewModelScope.launch {
        books.delete(book)
        onDone()
    }
}

/** Sheet aberta com long-press num livro: marcar coleções, criar coleção, remover da biblioteca. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookActionsSheet(book: BookEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel(key = "book-actions-${book.id}") {
        BookActionsViewModel(book.id, context.appContainer)
    }
    val options by viewModel.options.collectAsStateWithLifecycle()
    var newName by rememberSaveable { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cream,
        contentColor = Ink,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .background(Ink, NeoDefaults.Shape),
            )
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                BookCover(book, Modifier.width(64.dp), titleOnPlaceholder = false)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        book.author ?: stringResource(R.string.book_unknown_author),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    BookStatus(book)
                }
            }

            Text(stringResource(R.string.book_actions_collections), style = MaterialTheme.typography.titleMedium)
            options.forEach { option ->
                CollectionCheckRow(option) { viewModel.toggle(option.id, !option.checked) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val create = {
                    viewModel.createAndAdd(newName)
                    newName = ""
                }
                NeoTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = stringResource(R.string.book_actions_new_collection),
                    onDone = create,
                    modifier = Modifier.weight(1f),
                )
                NeoButton(onClick = create, color = Cream, enabled = newName.isNotBlank()) {
                    Text(stringResource(R.string.action_create))
                }
            }

            Spacer(Modifier.height(4.dp))
            NeoButton(onClick = { confirmDelete = true }, color = Danger, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text(stringResource(R.string.book_actions_remove))
            }
        }
    }

    if (confirmDelete) {
        NeoDialog(
            title = stringResource(R.string.book_remove_confirm_title),
            confirmText = stringResource(R.string.action_delete),
            confirmColor = Danger,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(book, onDismiss)
            },
            onDismiss = { confirmDelete = false },
        ) {
            Text(stringResource(R.string.book_remove_confirm_body, book.title), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CollectionCheckRow(option: CollectionOption, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .background(if (option.checked) Ink else Cream, NeoDefaults.Shape)
                .neoBorder(),
            contentAlignment = Alignment.Center,
        ) {
            if (option.checked) Icon(Icons.Filled.Check, contentDescription = null, tint = Cream, modifier = Modifier.size(18.dp))
        }
        Text(option.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

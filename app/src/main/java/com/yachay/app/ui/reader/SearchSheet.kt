package com.yachay.app.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yachay.app.R
import com.yachay.app.ui.components.NeoPressable
import com.yachay.app.ui.components.NeoSheet
import com.yachay.app.ui.components.NeoTextField
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.Paper
import com.yachay.app.ui.theme.tabular

/**
 * Busca no livro: campo no topo e as ocorrências com o termo em negrito. Toque num resultado
 * chama [onOpen] (o leitor fecha a sheet e vai até lá).
 */
@Composable
fun <T> SearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    state: SearchState<T>,
    onOpen: (SearchHit<T>) -> Unit,
    onDismiss: () -> Unit,
) {
    NeoSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val focus = remember { FocusRequester() }
            NeoTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            LaunchedEffect(Unit) { focus.requestFocus() }

            val status = when {
                state.query.isEmpty() -> null
                state.searching && state.hits.isEmpty() -> stringResource(R.string.search_running)
                state.hits.isEmpty() -> stringResource(R.string.search_empty, state.query)
                else -> pluralStringResource(R.plurals.search_results, state.hits.size, state.hits.size) +
                    if (state.searching) " · " + stringResource(R.string.search_running) else ""
            }
            status?.let {
                Text(it, style = MaterialTheme.typography.titleSmall.tabular(), color = if (state.hits.isEmpty()) InkMuted else Ink)
            }

            LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.hits) { _, hit ->
                    NeoPressable(onClick = { onOpen(hit) }, modifier = Modifier.fillMaxWidth(), color = Paper, shadow = 3.dp) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(hit.label, style = MaterialTheme.typography.labelMedium.tabular(), color = InkMuted)
                            Text(
                                buildAnnotatedString {
                                    val start = hit.match.first.coerceIn(0, hit.snippet.length)
                                    val end = (hit.match.last + 1).coerceIn(start, hit.snippet.length)
                                    append(hit.snippet.substring(0, start))
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(hit.snippet.substring(start, end)) }
                                    append(hit.snippet.substring(end))
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                            )
                        }
                    }
                }
            }
        }
    }
}

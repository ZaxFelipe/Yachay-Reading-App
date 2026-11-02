package com.yachay.app.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yachay.app.R
import com.yachay.app.data.db.BookEntity
import com.yachay.app.domain.BookFormat
import com.yachay.app.domain.ReadingProgress
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Purple
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.InkMuted
import androidx.compose.material3.LocalContentColor
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Paper
import com.yachay.app.ui.theme.CreamDeep
import com.yachay.app.ui.theme.Halftone
import com.yachay.app.ui.theme.PurpleDeep
import com.yachay.app.ui.theme.diagonalStripes
import com.yachay.app.ui.theme.halftone
import com.yachay.app.ui.theme.shadeFor
import androidx.compose.ui.draw.rotate
import com.yachay.app.ui.theme.contentColorFor
import com.yachay.app.ui.theme.neoBorder
import com.yachay.app.ui.theme.placeholderColorFor
import com.yachay.app.ui.theme.tabular
import java.io.File
import org.json.JSONObject

/** Livro aberto alguma vez e ainda não concluído. */
val BookEntity.isReading: Boolean get() = lastOpenedAt != null && !ReadingProgress.isDone(progress)

/** Nunca aberto: mostra "Novo" em vez de uma barra vazia em 0%. */
val BookEntity.isNew: Boolean get() = lastOpenedAt == null && progress == 0f

/** Capa 2:3 com contorno; sem imagem, vira um bloco colorido com o título. */
@Composable
fun BookCover(
    book: BookEntity,
    modifier: Modifier = Modifier,
    titleOnPlaceholder: Boolean = true,
    /** Selo "Lido!" no canto quando o livro foi concluído (só na estante). */
    showDoneBadge: Boolean = false,
) {
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .neoBorder(width = 2.dp)
            .clip(NeoDefaults.Shape),
    ) {
        if (book.coverPath != null) {
            AsyncImage(
                model = File(book.coverPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Sem capa: bloco chapado com halftone no canto e um brilho, como os blocos de ref2.
            val color = placeholderColorFor(book.id)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .halftone(Halftone(shadeFor(color), Alignment.TopEnd, spacing = 7.dp, maxDot = 2.8.dp, reach = 0.85f))
                    .padding(10.dp),
            ) {
                if (titleOnPlaceholder) {
                    Sparkle(
                        Modifier.align(Alignment.TopStart),
                        extent = 16.dp,
                        color = contentColorFor(color),
                    )
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColorFor(color),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
        }
        if (showDoneBadge && ReadingProgress.isDone(book.progress)) {
            BurstBadge(
                stringResource(R.string.book_badge_done),
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .rotate(-12f),
            )
        }
    }
}

/**
 * Livro na estante: a capa é o objeto (contorno + sombra dura, afunda ao tocar) e o texto fica
 * solto embaixo, sem cartão em volta. Capa e texto são um único alvo de toque.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick,
        ),
    ) {
        NeoPressable(
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interaction,
            shadow = 5.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BookCover(book, Modifier.fillMaxWidth(), showDoneBadge = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            book.author ?: stringResource(R.string.book_unknown_author),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        BookStatus(book)
    }
}

/** Linha de estado: "Novo · PDF · 30 págs" antes da primeira leitura; depois, a barra de progresso. */
@Composable
fun BookStatus(book: BookEntity, modifier: Modifier = Modifier) {
    if (book.isNew) {
        Text(
            stringResource(R.string.book_meta_new, formatMeta(book)),
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = LocalContentColor.current.copy(alpha = 0.72f),
            modifier = modifier,
        )
    } else {
        NeoProgressBar(progress = book.progress, height = 10.dp, modifier = modifier)
    }
}

@Composable
private fun formatMeta(book: BookEntity): String {
    val pages = book.totalPages
    return if (book.format == BookFormat.PDF && pages != null) {
        stringResource(R.string.book_meta_pages, book.format.name, pages)
    } else {
        book.format.name
    }
}

/**
 * "Página 13 de 30 · faltam 17" (PDF) ou "Em 40% do livro" (EPUB): onde o leitor parou, em
 * palavras. Se ele voltou para trás, acrescenta "· lido até 50%" — a barra mostra o máximo lido.
 */
@Composable
fun positionLabel(book: BookEntity): String {
    val pages = book.totalPages
    val page = book.lastPosition?.toIntOrNull()
    val current = currentFraction(book)
    val base = if (book.format == BookFormat.PDF && pages != null && page != null) {
        val number = page + 1
        val left = pages - number
        if (left > 0) {
            stringResource(R.string.book_position_pdf, number, pages, left)
        } else {
            stringResource(R.string.book_position_pdf_last, number, pages)
        }
    } else if (current != null) {
        stringResource(R.string.book_position_epub, ReadingProgress.percent(current))
    } else {
        stringResource(R.string.book_position_percent, ReadingProgress.percent(book.progress))
    }
    val furthest = ReadingProgress.percent(book.progress)
    return if (current != null && furthest > ReadingProgress.percent(current)) {
        stringResource(R.string.book_position_furthest, base, furthest)
    } else {
        base
    }
}

/** Posição atual como fração: página salva (PDF) ou `totalProgression` do Locator (EPUB). */
private fun currentFraction(book: BookEntity): Float? {
    val position = book.lastPosition ?: return null
    return when (book.format) {
        BookFormat.PDF -> {
            val pages = book.totalPages ?: return null
            position.toIntOrNull()?.let { ReadingProgress.forPdf(it, pages) }
        }
        BookFormat.EPUB -> runCatching {
            JSONObject(position).optJSONObject("locations")?.optDouble("totalProgression")
                ?.takeUnless { it.isNaN() }
                ?.let { ReadingProgress.forEpub(it) }
        }.getOrNull()
    }
}

@Composable
private fun lastOpenedLabel(book: BookEntity): String? {
    val opened = book.lastOpenedAt ?: return null
    val relative = DateUtils.getRelativeTimeSpanString(
        opened,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
    ).toString().lowercase()
    return stringResource(R.string.book_last_opened, relative)
}

/**
 * O bloco que responde "onde eu parei?": o maior elemento da Biblioteca, um campo roxo com o
 * destaque em limão (como as ilustrações de referência). O cartão inteiro é o botão de continuar.
 */
@Composable
fun ReadingNowCard(
    book: BookEntity,
    onContinue: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoPressable(
        onClick = onContinue,
        onLongClick = onLongClick,
        color = Purple,
        shadow = 6.dp,
        modifier = modifier.fillMaxWidth(),
        halftone = Halftone(PurpleDeep, Alignment.BottomEnd, spacing = 9.dp, maxDot = 4.dp, reach = 0.6f),
    ) {
        ImpactMarks(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            extent = 32.dp,
            color = Lime,
            mirrored = true,
        )
        Row(
            Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BookCover(book, Modifier.width(100.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author ?: stringResource(R.string.book_unknown_author),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnPurpleMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    positionLabel(book),
                    style = MaterialTheme.typography.labelLarge.tabular(),
                )
                Spacer(Modifier.height(6.dp))
                NeoProgressBar(progress = book.progress, height = 14.dp, fillColor = Lime, showLabel = false)
                lastOpenedLabel(book)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = OnPurpleMuted)
                }
                Spacer(Modifier.height(12.dp))
                ContinueLabel()
            }
        }
    }
}

/** Texto secundário sobre roxo: creme recuado (≈5:1). */
private val OnPurpleMuted = Cream.copy(alpha = 0.82f)

/** Parece um botão limão; o clique é do cartão inteiro, que já é o botão. */
@Composable
private fun ContinueLabel() {
    Row(
        Modifier
            .background(Lime, NeoDefaults.Shape)
            .neoBorder(width = 2.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.library_continue), style = MaterialTheme.typography.labelLarge, color = Ink)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
    }
}

/** Livro em andamento secundário: capa pequena + título + barra, numa faixa horizontal. */
@Composable
fun AlsoReadingCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** true quando o cartão ocupa a largura toda (o texto cresce em vez de ter largura fixa). */
    fill: Boolean = false,
) {
    NeoPressable(onClick = onClick, onLongClick = onLongClick, modifier = modifier, shadow = 4.dp) {
        Row(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book, Modifier.width(48.dp), titleOnPlaceholder = false)
            Column(
                if (fill) Modifier.weight(1f) else Modifier.width(150.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                NeoProgressBar(progress = book.progress, height = 8.dp)
            }
        }
    }
}

/**
 * Até três capas em leque, sobre a cor da coleção. Sem livros, um contorno de capa vazio —
 * o espaço onde o primeiro livro vai entrar.
 */
@Composable
fun CoverFan(books: List<BookEntity>, modifier: Modifier = Modifier, coverWidth: Dp = 44.dp, step: Dp = 18.dp) {
    val shown = books.take(3)
    Box(modifier.width(coverWidth + step * 2)) {
        if (shown.isEmpty()) {
            Box(
                Modifier
                    .width(coverWidth)
                    .aspectRatio(2f / 3f)
                    .background(Paper, NeoDefaults.Shape)
                    .diagonalStripes(CreamDeep)
                    .neoBorder(width = 2.dp),
            )
        }
        shown.forEachIndexed { index, book ->
            BookCover(
                book,
                Modifier
                    .offset(x = step * index, y = if (index % 2 == 1) 4.dp else 0.dp)
                    .width(coverWidth),
                titleOnPlaceholder = false,
            )
        }
    }
}

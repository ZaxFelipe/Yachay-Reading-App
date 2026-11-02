package com.yachay.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yachay.app.R
import com.yachay.app.domain.ReadingProgress
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Purple
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.InkMuted
import com.yachay.app.ui.theme.tabular
import com.yachay.app.ui.theme.NeoDefaults
import com.yachay.app.ui.theme.PercentTextStyle
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.CreamMuted
import com.yachay.app.ui.theme.PurpleDeep
import com.yachay.app.ui.theme.shadowFor
import com.yachay.app.ui.theme.CreamDeep
import com.yachay.app.ui.theme.Halftone
import com.yachay.app.ui.theme.halftone
import com.yachay.app.ui.theme.marker
import com.yachay.app.ui.theme.shadeFor
import com.yachay.app.ui.theme.Paper
import com.yachay.app.ui.theme.contentColorFor
import com.yachay.app.ui.theme.neoBorder
import com.yachay.app.ui.theme.neoShadow
import androidx.compose.ui.res.stringResource

/**
 * Superfície estática neobrutalista: fundo chapado, contorno [Ink] e sombra dura.
 * O espaço da sombra é reservado com padding, então o tamanho externo já inclui a sombra.
 */
@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    color: Color = Cream,
    shape: Shape = NeoDefaults.Shape,
    shadow: Dp = NeoDefaults.ShadowOffset,
    /** Textura de pontos num canto (opcional), desenhada por baixo do contorno. */
    halftone: Halftone? = null,
    shadowColor: Color = shadowFor(color),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .padding(end = shadow, bottom = shadow)
            .neoShadow(shadow, shape, shadowColor)
            .background(color, shape)
            .neoBorder(shape = shape)
            .clip(shape)
            .halftone(halftone),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColorFor(color)) {
            content()
        }
    }
}

/**
 * Base de todo elemento clicável: ao pressionar, o elemento "afunda" sobre a própria sombra.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeoPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    color: Color = Cream,
    shape: Shape = NeoDefaults.Shape,
    shadow: Dp = NeoDefaults.ShadowOffset,
    enabled: Boolean = true,
    role: Role = Role.Button,
    /** Passe uma fonte compartilhada para "afundar" junto com outra área clicável (ex.: capa + título). */
    interactionSource: MutableInteractionSource? = null,
    /** Textura de pontos num canto (opcional), desenhada por baixo do contorno. */
    halftone: Halftone? = null,
    shadowColor: Color = shadowFor(color),
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed && enabled) shadow else 0.dp, label = "neoPress")
    val background = if (enabled) color else color.copy(alpha = 0.45f)

    Box(
        modifier
            .padding(end = shadow, bottom = shadow)
            .offset(x = shift, y = shift)
            .neoShadow(if (enabled) shadow - shift else 0.dp, shape, shadowColor)
            .background(background, shape)
            .neoBorder(shape = shape)
            .clip(shape)
            .halftone(halftone)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = role,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColorFor(color)) {
            content()
        }
    }
}

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Lime,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    NeoPressable(onClick = onClick, modifier = modifier, color = color, enabled = enabled) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
            Row(
                Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Cream,
    size: Dp = 44.dp,
) {
    NeoPressable(onClick = onClick, modifier = modifier, color = color, shadow = 3.dp) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .padding(10.dp),
        )
    }
}

@Composable
fun NeoFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Lime,
) {
    NeoPressable(onClick = onClick, modifier = modifier, color = color, shadow = 6.dp) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(64.dp)
                .padding(18.dp),
        )
    }
}

@Composable
fun NeoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Seleção em limão (o destaque das referências); não selecionado em roxo profundo.
    selectedColor: Color = Lime,
) {
    NeoPressable(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) selectedColor else PurpleDeep,
        shadow = if (selected) 3.dp else 0.dp,
        role = Role.Tab,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * Barra de progresso de leitura: trilho de papel com contorno, preenchimento [Purple]
 * que vira [Lime] quando o livro é concluído, e o percentual em negrito ao lado
 * (na cor de conteúdo do lugar onde a barra está — creme dentro de uma barra roxa).
 */
@Composable
fun NeoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    showLabel: Boolean = true,
    /** Purple por padrão; sobre um fundo roxo use Lime para o preenchimento continuar visível. */
    fillColor: Color = Purple,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    val done = ReadingProgress.isDone(progress)
    val fill = if (done) Lime else fillColor

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(height)
                .background(Paper, NeoDefaults.Shape)
                .neoBorder(width = 2.dp)
                .clip(NeoDefaults.Shape),
        ) {
            if (animated > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .background(fill)
                        .drawBehind {
                            // Linha divisória dura na ponta do preenchimento.
                            val x = size.width - 1.dp.toPx()
                            drawLine(Ink, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                        },
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (done) stringResource(R.string.book_progress_done) else "${ReadingProgress.percent(progress)}%",
                style = PercentTextStyle,
                color = LocalContentColor.current,
            )
        }
    }
}

/**
 * Barra superior: campo roxo da marca com contorno inferior grosso. O conteúdo
 * (título, linha de baixo, barra de progresso) herda a cor legível via LocalContentColor.
 */
@Composable
fun NeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Purple,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable () -> Unit)? = null,
    /** Pontos no canto superior direito, no tom escuro da própria barra (como em ref1). */
    halftone: Halftone? = Halftone(shadeFor(color), Alignment.TopEnd, spacing = 9.dp, maxDot = 3.5.dp, reach = 0.5f),
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color)
            .halftone(halftone)
            .drawBehind {
                val stroke = NeoDefaults.BorderWidth.toPx()
                drawLine(Ink, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
            }
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColorFor(color)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navigationIcon != null) {
                    navigationIcon()
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
            }
            if (bottom != null) {
                Spacer(Modifier.height(10.dp))
                bottom()
            }
        }
    }
}

/** Texto secundário dentro de uma [NeoTopBar]: a cor de conteúdo da barra, um pouco recuada. */
@Composable
fun TopBarSubtitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalContentColor.current.copy(alpha = 0.85f),
    )
}

@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(Ink),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .background(Paper, NeoDefaults.Shape)
                    .neoBorder()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = textStyle, color = Ink.copy(alpha = 0.45f))
                inner()
            }
        },
    )
}

@Composable
fun NeoDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = Lime,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoSurface(shadow = 6.dp, halftone = Halftone(CreamDeep, Alignment.BottomEnd, reach = 0.45f)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                content()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    NeoButton(onClick = onDismiss, color = Cream) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    NeoButton(onClick = onConfirm, color = confirmColor, enabled = confirmEnabled) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

/**
 * Bloco de estado vazio: cartão colorido com título forte, uma frase e, quando houver,
 * a ação que resolve o vazio — no lugar onde o conteúdo vai aparecer.
 */
@Composable
fun NeoEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    color: Color = Purple,
    action: (@Composable () -> Unit)? = null,
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        shadow = 6.dp,
        halftone = Halftone(shadeFor(color), Alignment.BottomEnd, spacing = 9.dp, maxDot = 4.dp, reach = 0.6f),
    ) {
        // Fim do texto recuado para não passar por baixo das marcas de impacto.
        Column(
            Modifier.padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 56.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body, style = MaterialTheme.typography.bodyLarge)
            if (action != null) {
                Spacer(Modifier.height(8.dp))
                action()
            }
        }
        ImpactMarks(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp),
            color = contentColorFor(color),
            mirrored = true,
        )
        Sparkle(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
            extent = 26.dp,
            color = Lime,
            outline = Ink,
        )
    }
}

/** Marca do Yachay: um "Y" em bloco limão, com contorno e sombra dura, como um selo sobre o roxo. */
@Composable
fun YachayMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .neoShadow(3.dp, color = Ink)
            .size(size)
            .background(Lime, NeoDefaults.Shape)
            .neoBorder(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Y", style = MaterialTheme.typography.titleLarge, color = Ink)
    }
}

/** Título de seção com uma contagem opcional alinhada à direita (algarismos tabulares). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
        // O Box ocupa a largura; o Text fica do tamanho das palavras para o marca-texto acompanhar.
        Box(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.marker(Lime),
            )
        }
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = CreamMuted,
            )
        }
    }
}

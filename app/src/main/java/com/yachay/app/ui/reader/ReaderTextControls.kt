package com.yachay.app.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yachay.app.R
import com.yachay.app.domain.FontScale
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.PageMargins
import com.yachay.app.domain.ReaderFont
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.TextAlignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.components.NeoButton
import com.yachay.app.ui.components.NeoChip
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.tabular

/*
 * Controles de texto do EPUB, usados em dois lugares com a mesma preferência:
 * a sheet "Aa" dentro do leitor e a seção de Leitura em Ajustes.
 */

/** `A−  120%  A+`: passos de 10%; o botão do limite fica desabilitado. */
@Composable
fun FontScaleStepper(scale: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val decrease = stringResource(R.string.reader_font_decrease)
    val increase = stringResource(R.string.reader_font_increase)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        NeoButton(
            onClick = { onChange(FontScale.next(scale, step = -1)) },
            color = Cream,
            enabled = FontScale.canStep(scale, step = -1),
        ) {
            Text("A−", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { contentDescription = decrease })
        }
        Text(
            stringResource(R.string.reader_font_scale_value, FontScale.percent(scale)),
            style = MaterialTheme.typography.titleLarge.tabular(),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        NeoButton(
            onClick = { onChange(FontScale.next(scale, step = +1)) },
            color = Cream,
            enabled = FontScale.canStep(scale, step = +1),
        ) {
            Text("A+", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = increase })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineSpacingChips(selected: LineSpacing, onSelect: (LineSpacing) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LineSpacing.entries.forEach { spacing ->
            val label = when (spacing) {
                LineSpacing.COMPACT -> R.string.line_spacing_compact
                LineSpacing.NORMAL -> R.string.line_spacing_normal
                LineSpacing.WIDE -> R.string.line_spacing_wide
            }
            NeoChip(stringResource(label), selected = spacing == selected, onClick = { onSelect(spacing) })
        }
    }
}

/** Cada chip escrito na própria fonte ("Legível" usa a OpenDyslexic que o Readium embute). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderFontChips(selected: ReaderFont, onSelect: (ReaderFont) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val legible = remember {
        runCatching { FontFamily(Font("readium/fonts/OpenDyslexic-Regular.otf", context.assets)) }.getOrNull()
    }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReaderFont.entries.forEach { font ->
            val (label, family) = when (font) {
                ReaderFont.PUBLISHER -> R.string.reader_font_publisher to null
                ReaderFont.SERIF -> R.string.reader_font_serif to FontFamily.Serif
                ReaderFont.SANS_SERIF -> R.string.reader_font_sans to FontFamily.SansSerif
                ReaderFont.LEGIBLE -> R.string.reader_font_legible to legible
            }
            NeoChip(stringResource(label), selected = font == selected, onClick = { onSelect(font) }, fontFamily = family)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageMarginsChips(selected: PageMargins, onSelect: (PageMargins) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PageMargins.entries.forEach { margins ->
            val label = when (margins) {
                PageMargins.NARROW -> R.string.reader_margins_narrow
                PageMargins.NORMAL -> R.string.reader_margins_normal
                PageMargins.WIDE -> R.string.reader_margins_wide
            }
            NeoChip(stringResource(label), selected = margins == selected, onClick = { onSelect(margins) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextAlignmentChips(selected: TextAlignment, onSelect: (TextAlignment) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextAlignment.entries.forEach { align ->
            val label = when (align) {
                TextAlignment.PUBLISHER -> R.string.reader_align_publisher
                TextAlignment.LEFT -> R.string.reader_align_left
                TextAlignment.JUSTIFY -> R.string.reader_align_justify
            }
            NeoChip(stringResource(label), selected = align == selected, onClick = { onSelect(align) })
        }
    }
}

/**
 * Todos os controles de texto do EPUB, na mesma ordem nos dois lugares. O aviso de estilos
 * da editora só aparece quando alguma escolha os desliga.
 */
@Composable
fun EpubTextControls(
    preferences: ReaderPreferences,
    onFontScale: (Float) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onFont: (ReaderFont) -> Unit,
    onMargins: (PageMargins) -> Unit,
    onTextAlign: (TextAlignment) -> Unit,
    noteColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reader_font_size), style = MaterialTheme.typography.titleMedium)
        FontScaleStepper(preferences.fontScale, onFontScale)
        ControlTitle(R.string.reader_font_family)
        ReaderFontChips(preferences.font, onFont)
        ControlTitle(R.string.reader_line_spacing)
        LineSpacingChips(preferences.lineSpacing, onLineSpacing)
        ControlTitle(R.string.reader_margins)
        PageMarginsChips(preferences.margins, onMargins)
        ControlTitle(R.string.reader_align)
        TextAlignmentChips(preferences.textAlign, onTextAlign)
        if (preferences.overridesPublisherStyles) {
            Text(
                stringResource(R.string.reader_publisher_styles_note),
                style = MaterialTheme.typography.bodySmall,
                color = noteColor,
            )
        }
    }
}

@Composable
private fun ControlTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

/** Atalho para o tema de leitura (a mesma escolha de Ajustes). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingThemeChips(selected: ReadingTheme, onSelect: (ReadingTheme) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReadingTheme.entries.forEach { theme ->
            val label = when (theme) {
                ReadingTheme.PAPER -> R.string.theme_paper
                ReadingTheme.NIGHT -> R.string.theme_night
            }
            NeoChip(stringResource(label), selected = theme == selected, onClick = { onSelect(theme) })
        }
    }
}

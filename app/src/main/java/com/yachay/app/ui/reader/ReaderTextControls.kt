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

package com.yachay.app.ui.reader.epub

import androidx.compose.ui.graphics.toArgb
import com.yachay.app.domain.FontScale
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.colors
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Theme

/**
 * Preferências do Yachay no formato do Readium. O tema LIGHT/DARK cuida de links e seleção.
 * `fontSize` funciona com os estilos da editora; `lineHeight` não — então só os desligamos
 * quando a pessoa escolhe um espaçamento diferente do Normal (o padrão respeita o livro).
 */
fun ReaderPreferences.toEpubPreferences(): EpubPreferences {
    val colors = theme.colors()
    val customSpacing = lineSpacing != LineSpacing.NORMAL
    return EpubPreferences(
        theme = if (theme == ReadingTheme.NIGHT) Theme.DARK else Theme.LIGHT,
        backgroundColor = ReadiumColor(colors.page.toArgb()),
        textColor = ReadiumColor(colors.text.toArgb()),
        // Pelo percentual inteiro: 1.3f.toDouble() viraria 1.2999999523.
        fontSize = FontScale.percent(fontScale) / 100.0,
        lineHeight = lineSpacing.lineHeight,
        publisherStyles = if (customSpacing) false else null,
    )
}

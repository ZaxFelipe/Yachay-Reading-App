package com.yachay.app.ui.reader.epub

import androidx.compose.ui.graphics.toArgb
import com.yachay.app.domain.FontScale
import com.yachay.app.domain.ReaderFont
import com.yachay.app.domain.TextAlignment
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.reader.colors
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme

/**
 * Preferências do Yachay no formato do Readium. O tema LIGHT/DARK cuida de links e seleção.
 * `fontSize`, `fontFamily` e `pageMargins` funcionam com os estilos da editora; `lineHeight` e
 * `textAlign` não (conferido no bytecode da 3.0.3) — então só os desligamos quando a pessoa
 * foge do padrão nesses dois ([ReaderPreferences.overridesPublisherStyles]).
 */
fun ReaderPreferences.toEpubPreferences(): EpubPreferences {
    val colors = theme.colors()
    return EpubPreferences(
        theme = if (theme == ReadingTheme.NIGHT) Theme.DARK else Theme.LIGHT,
        backgroundColor = ReadiumColor(colors.page.toArgb()),
        textColor = ReadiumColor(colors.text.toArgb()),
        // Pelo percentual inteiro: 1.3f.toDouble() viraria 1.2999999523.
        fontSize = FontScale.percent(fontScale) / 100.0,
        lineHeight = lineSpacing.lineHeight,
        fontFamily = when (font) {
            ReaderFont.PUBLISHER -> null
            ReaderFont.SERIF -> FontFamily.SERIF
            ReaderFont.SANS_SERIF -> FontFamily.SANS_SERIF
            ReaderFont.LEGIBLE -> FontFamily.OPEN_DYSLEXIC
        },
        pageMargins = margins.factor,
        textAlign = when (textAlign) {
            TextAlignment.PUBLISHER -> null
            TextAlignment.LEFT -> TextAlign.LEFT
            TextAlignment.JUSTIFY -> TextAlign.JUSTIFY
        },
        publisherStyles = if (overridesPublisherStyles) false else null,
    )
}

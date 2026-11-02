package com.yachay.app.ui.reader.epub

import androidx.compose.ui.graphics.toArgb
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.PageMargins
import com.yachay.app.domain.ReaderFont
import com.yachay.app.domain.TextAlignment
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Night
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme

class EpubPreferencesMappingTest {

    @Test
    fun `defaults keep the publisher styles`() {
        val prefs = ReaderPreferences().toEpubPreferences()
        assertEquals(Theme.LIGHT, prefs.theme)
        assertEquals(ReadiumColor(Cream.toArgb()), prefs.backgroundColor)
        assertEquals(ReadiumColor(Ink.toArgb()), prefs.textColor)
        assertEquals(1.0, prefs.fontSize!!, 0.0)
        assertNull(prefs.lineHeight)
        assertNull(prefs.fontFamily)
        assertNull(prefs.pageMargins)
        assertNull(prefs.textAlign)
        assertNull(prefs.publisherStyles)
    }

    @Test
    fun `font family and margins keep the publisher styles`() {
        ReaderFont.entries.forEach { font ->
            PageMargins.entries.forEach { margins ->
                val prefs = ReaderPreferences(font = font, margins = margins).toEpubPreferences()
                assertNull("$font/$margins", prefs.publisherStyles)
                assertEquals(margins.factor, prefs.pageMargins)
            }
        }
        assertEquals(FontFamily.SERIF, ReaderPreferences(font = ReaderFont.SERIF).toEpubPreferences().fontFamily)
        assertEquals(FontFamily.SANS_SERIF, ReaderPreferences(font = ReaderFont.SANS_SERIF).toEpubPreferences().fontFamily)
        assertEquals(FontFamily.OPEN_DYSLEXIC, ReaderPreferences(font = ReaderFont.LEGIBLE).toEpubPreferences().fontFamily)
        assertEquals(2.0, ReaderPreferences(margins = PageMargins.WIDE).toEpubPreferences().pageMargins!!, 0.0)
    }

    @Test
    fun `text alignment turns off publisher styles only when not from the book`() {
        assertNull(ReaderPreferences(textAlign = TextAlignment.PUBLISHER).toEpubPreferences().publisherStyles)
        val left = ReaderPreferences(textAlign = TextAlignment.LEFT).toEpubPreferences()
        assertEquals(TextAlign.LEFT, left.textAlign)
        assertEquals(false, left.publisherStyles)
        val justify = ReaderPreferences(textAlign = TextAlignment.JUSTIFY).toEpubPreferences()
        assertEquals(TextAlign.JUSTIFY, justify.textAlign)
        assertEquals(false, justify.publisherStyles)
    }

    @Test
    fun `maps theme, font size and spacing`() {
        val prefs = ReaderPreferences(ReadingTheme.NIGHT, fontScale = 1.3f, lineSpacing = LineSpacing.WIDE)
            .toEpubPreferences()
        assertEquals(Theme.DARK, prefs.theme)
        assertEquals(ReadiumColor(Night.toArgb()), prefs.backgroundColor)
        assertEquals(ReadiumColor(Cream.toArgb()), prefs.textColor)
        assertEquals(1.3, prefs.fontSize!!, 0.0)
        assertEquals(1.8, prefs.lineHeight!!, 0.0)
        // lineHeight só vale no Readium com os estilos da editora desligados.
        assertEquals(false, prefs.publisherStyles)
    }
}

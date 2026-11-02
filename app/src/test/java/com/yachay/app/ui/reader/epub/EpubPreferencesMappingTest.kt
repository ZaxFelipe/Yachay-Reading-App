package com.yachay.app.ui.reader.epub

import androidx.compose.ui.graphics.toArgb
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Night
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.navigator.preferences.Color as ReadiumColor
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
        assertNull(prefs.publisherStyles)
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

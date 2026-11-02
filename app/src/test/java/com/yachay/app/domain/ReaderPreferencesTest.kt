package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPreferencesTest {

    @Test
    fun `font scale steps by 10 percent without float drift`() {
        assertEquals(1.1f, FontScale.next(1f, step = +1), 0f)
        assertEquals(0.9f, FontScale.next(1f, step = -1), 0f)
        // Somar 0,1 dez vezes dá 1.9999999 em float; o passo arredonda para múltiplos de 0,1.
        var scale = 1f
        repeat(10) { scale = FontScale.next(scale, step = +1) }
        assertEquals(2f, scale, 0f)
        assertEquals(1.2f, FontScale.next(1.0999999f, step = +1), 0f)
    }

    @Test
    fun `font scale respects the limits`() {
        assertEquals(FontScale.MAX, FontScale.next(FontScale.MAX, step = +1), 0f)
        assertEquals(FontScale.MIN, FontScale.next(FontScale.MIN, step = -1), 0f)
        assertFalse(FontScale.canStep(FontScale.MAX, step = +1))
        assertFalse(FontScale.canStep(FontScale.MIN, step = -1))
        assertTrue(FontScale.canStep(1f, step = +1))
        assertTrue(FontScale.canStep(1f, step = -1))
    }

    @Test
    fun `invalid stored font scale falls back to default`() {
        assertEquals(FontScale.DEFAULT, FontScale.sanitize(null), 0f)
        assertEquals(FontScale.DEFAULT, FontScale.sanitize(Float.NaN), 0f)
        assertEquals(FontScale.DEFAULT, FontScale.sanitize(9f), 0f)
        assertEquals(FontScale.DEFAULT, FontScale.sanitize(0.1f), 0f)
        assertEquals(1.3f, FontScale.sanitize(1.3f), 0f)
    }

    @Test
    fun `percent shows the scale as an integer`() {
        assertEquals(100, FontScale.percent(1f))
        assertEquals(70, FontScale.percent(0.7f))
        assertEquals(250, FontScale.percent(2.5f))
    }

    @Test
    fun `line spacing parses names and falls back to normal`() {
        assertEquals(LineSpacing.WIDE, LineSpacing.fromName("WIDE"))
        assertEquals(LineSpacing.NORMAL, LineSpacing.fromName(null))
        assertEquals(LineSpacing.NORMAL, LineSpacing.fromName("enorme"))
    }
}

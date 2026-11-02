package com.yachay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksTest {

    @Test
    fun `same screen means same chapter and same start`() {
        assertTrue(Bookmarks.sameScreen(EpubSpot("c1.xhtml", 0.25), EpubSpot("c1.xhtml", 0.250001)))
        assertFalse(Bookmarks.sameScreen(EpubSpot("c1.xhtml", 0.25), EpubSpot("c1.xhtml", 0.30)))
        assertFalse(Bookmarks.sameScreen(EpubSpot("c1.xhtml", 0.25), EpubSpot("c2.xhtml", 0.25)))
    }

    @Test
    fun `list follows reading order`() {
        val spots = listOf(
            EpubSpot("c2.xhtml", 0.1, 0.55),
            EpubSpot("c1.xhtml", 0.9, 0.30),
            EpubSpot("c1.xhtml", 0.2, 0.05),
            EpubSpot("cx.xhtml", 0.0, null),
        )
        assertEquals(
            listOf(0.05, 0.30, 0.55, null),
            spots.sortedWith(Bookmarks.EpubOrder).map { it.totalProgression },
        )
    }
}

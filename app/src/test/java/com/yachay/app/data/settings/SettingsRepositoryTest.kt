package com.yachay.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yachay.app.domain.FontScale
import com.yachay.app.domain.LibrarySort
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.PageMargins
import com.yachay.app.domain.ReaderFont
import com.yachay.app.domain.TextAlignment
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val store: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb").also { it.delete() } }
    }

    private val repository by lazy { SettingsRepository(store) }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `defaults when nothing was saved`() = runBlocking {
        assertEquals(ReaderPreferences(), repository.currentReaderPreferences())
    }

    @Test
    fun `saves and reads font scale, spacing and theme together`() = runBlocking {
        repository.setFontScale(1.4f)
        repository.setLineSpacing(LineSpacing.COMPACT)
        repository.setReadingTheme(ReadingTheme.NIGHT)
        assertEquals(
            ReaderPreferences(ReadingTheme.NIGHT, fontScale = 1.4f, lineSpacing = LineSpacing.COMPACT),
            repository.currentReaderPreferences(),
        )
    }

    @Test
    fun `corrupted values fall back to defaults`() = runBlocking {
        store.edit {
            it[floatPreferencesKey("epub_font_scale")] = 42f
            it[stringPreferencesKey("epub_line_spacing")] = "???"
        }
        val prefs = repository.currentReaderPreferences()
        assertEquals(FontScale.DEFAULT, prefs.fontScale, 0f)
        assertEquals(LineSpacing.NORMAL, prefs.lineSpacing)
    }

    @Test
    fun `library sort is saved and falls back to default`() = runBlocking {
        assertEquals(LibrarySort.ADDED, repository.librarySort.first())
        repository.setLibrarySort(LibrarySort.AUTHOR)
        assertEquals(LibrarySort.AUTHOR, repository.librarySort.first())
        store.edit { it[stringPreferencesKey("library_sort")] = "SIZE" }
        assertEquals(LibrarySort.ADDED, repository.librarySort.first())
    }

    @Test
    fun `saves and reads font family, margins and alignment`() = runBlocking {
        repository.setFont(ReaderFont.LEGIBLE)
        repository.setMargins(PageMargins.WIDE)
        repository.setTextAlign(TextAlignment.JUSTIFY)
        val prefs = repository.currentReaderPreferences()
        assertEquals(ReaderFont.LEGIBLE, prefs.font)
        assertEquals(PageMargins.WIDE, prefs.margins)
        assertEquals(TextAlignment.JUSTIFY, prefs.textAlign)
    }

    @Test
    fun `invalid font, margins and alignment fall back to the book`() = runBlocking {
        store.edit {
            it[stringPreferencesKey("epub_font_family")] = "Comic Sans"
            it[stringPreferencesKey("epub_margins")] = ""
            it[stringPreferencesKey("epub_text_align")] = "CENTER"
        }
        val prefs = repository.currentReaderPreferences()
        assertEquals(ReaderFont.PUBLISHER, prefs.font)
        assertEquals(PageMargins.NORMAL, prefs.margins)
        assertEquals(TextAlignment.PUBLISHER, prefs.textAlign)
    }
}

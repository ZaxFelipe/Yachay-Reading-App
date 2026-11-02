package com.yachay.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yachay.app.domain.FontScale
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
}

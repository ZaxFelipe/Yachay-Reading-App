package com.yachay.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yachay.app.domain.FontScale
import com.yachay.app.domain.LibrarySort
import com.yachay.app.domain.LineSpacing
import com.yachay.app.domain.PageMargins
import com.yachay.app.domain.ReaderFont
import com.yachay.app.domain.TextAlignment
import com.yachay.app.domain.ReaderPreferences
import com.yachay.app.domain.ReadingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preferências do usuário (DataStore). Uma instância por processo, via AppContainer.
 * Recebe o [DataStore] pronto para os testes usarem um arquivo temporário.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val readingTheme: Flow<ReadingTheme> = store.data
        .map { ReadingTheme.fromName(it[READING_THEME]) }
        .distinctUntilChanged()

    /** Tudo o que o leitor EPUB aplica junto (um Flow só, reenviado inteiro ao navegador). */
    val readerPreferences: Flow<ReaderPreferences> = store.data
        .map {
            ReaderPreferences(
                theme = ReadingTheme.fromName(it[READING_THEME]),
                fontScale = FontScale.sanitize(it[EPUB_FONT_SCALE]),
                lineSpacing = LineSpacing.fromName(it[EPUB_LINE_SPACING]),
                font = ReaderFont.fromName(it[EPUB_FONT_FAMILY]),
                margins = PageMargins.fromName(it[EPUB_MARGINS]),
                textAlign = TextAlignment.fromName(it[EPUB_TEXT_ALIGN]),
            )
        }
        .distinctUntilChanged()

    val librarySort: Flow<LibrarySort> = store.data
        .map { LibrarySort.fromName(it[LIBRARY_SORT]) }
        .distinctUntilChanged()

    suspend fun setLibrarySort(sort: LibrarySort) {
        store.edit { it[LIBRARY_SORT] = sort.name }
    }

    suspend fun currentReaderPreferences(): ReaderPreferences = readerPreferences.first()

    suspend fun setReadingTheme(theme: ReadingTheme) {
        store.edit { it[READING_THEME] = theme.name }
    }

    suspend fun setFontScale(scale: Float) {
        store.edit { it[EPUB_FONT_SCALE] = FontScale.sanitize(scale) }
    }

    suspend fun setLineSpacing(spacing: LineSpacing) {
        store.edit { it[EPUB_LINE_SPACING] = spacing.name }
    }

    suspend fun setFont(font: ReaderFont) {
        store.edit { it[EPUB_FONT_FAMILY] = font.name }
    }

    suspend fun setMargins(margins: PageMargins) {
        store.edit { it[EPUB_MARGINS] = margins.name }
    }

    suspend fun setTextAlign(align: TextAlignment) {
        store.edit { it[EPUB_TEXT_ALIGN] = align.name }
    }

    private companion object {
        val READING_THEME = stringPreferencesKey("reading_theme")
        val EPUB_FONT_SCALE = floatPreferencesKey("epub_font_scale")
        val EPUB_LINE_SPACING = stringPreferencesKey("epub_line_spacing")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val EPUB_FONT_FAMILY = stringPreferencesKey("epub_font_family")
        val EPUB_MARGINS = stringPreferencesKey("epub_margins")
        val EPUB_TEXT_ALIGN = stringPreferencesKey("epub_text_align")
    }
}

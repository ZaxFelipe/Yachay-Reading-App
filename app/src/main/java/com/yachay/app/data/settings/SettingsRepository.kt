package com.yachay.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yachay.app.domain.ReadingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Preferências do usuário (DataStore). Uma instância por processo, via AppContainer. */
class SettingsRepository(context: Context) {

    private val store = context.settingsStore

    val readingTheme: Flow<ReadingTheme> = store.data
        .map { ReadingTheme.fromName(it[READING_THEME]) }
        .distinctUntilChanged()

    suspend fun currentReadingTheme(): ReadingTheme = readingTheme.first()

    suspend fun setReadingTheme(theme: ReadingTheme) {
        store.edit { it[READING_THEME] = theme.name }
    }

    private companion object {
        val READING_THEME = stringPreferencesKey("reading_theme")
    }
}

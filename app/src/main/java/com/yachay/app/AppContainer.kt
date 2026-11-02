package com.yachay.app

import android.content.Context
import com.yachay.app.data.db.YachayDatabase
import com.yachay.app.data.importer.BookImporter
import com.yachay.app.data.importer.CoverStore
import com.yachay.app.data.importer.ReadiumServices
import com.yachay.app.data.repo.BookRepository
import com.yachay.app.data.repo.CollectionRepository
import com.yachay.app.data.settings.SettingsRepository
import com.yachay.app.data.settings.settingsStore
import com.yachay.app.ui.reader.epub.EpubSessions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** DI manual: todas as dependências de longa duração vivem aqui, uma instância por processo. */
class AppContainer(context: Context) {

    /** Escopo que sobrevive a telas/activities — usado para gravações que não podem ser canceladas. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = YachayDatabase.create(context)

    val coverStore = CoverStore(context)

    val readium = ReadiumServices(context)

    val bookRepository = BookRepository(database.bookDao(), coverStore, context.contentResolver)

    val collectionRepository = CollectionRepository(database.collectionDao())

    val bookImporter = BookImporter(context, database.bookDao(), coverStore, readium)

    val settings = SettingsRepository(context.settingsStore)

    val epubSessions = EpubSessions(readium, settings)
}

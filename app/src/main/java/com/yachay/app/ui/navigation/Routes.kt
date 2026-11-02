package com.yachay.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data object CollectionsRoute

@Serializable
data object SettingsRoute

/** Aberto pelo avatar no canto da Biblioteca. */
@Serializable
data object ProfileRoute

@Serializable
data class CollectionDetailRoute(val collectionId: Long)

/**
 * Leitor de páginas (PDF e CBZ). EPUB não tem rota: abre na EpubReaderActivity
 * (ver ui/reader/BookOpener.kt).
 */
@Serializable
data class PdfReaderRoute(val bookId: Long)

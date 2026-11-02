package com.yachay.app.domain

/** Ordem da estante "Todos os livros", salva em Ajustes (`library_sort`). */
enum class LibrarySort {
    ADDED,
    OPENED,
    TITLE,
    AUTHOR,
    PROGRESS;

    companion object {
        val Default = ADDED

        fun fromName(name: String?): LibrarySort = entries.firstOrNull { it.name == name } ?: Default
    }
}

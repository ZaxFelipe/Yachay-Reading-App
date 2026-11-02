package com.yachay.app.domain

/** Tema das páginas nos leitores (PDF e EPUB). Escolhido em Ajustes; o app em si é sempre escuro. */
enum class ReadingTheme {
    /** Página creme, texto em tinta. */
    PAPER,

    /** Página roxo-noite, texto creme. */
    NIGHT;

    companion object {
        val Default = PAPER

        fun fromName(name: String?): ReadingTheme = entries.firstOrNull { it.name == name } ?: Default
    }
}

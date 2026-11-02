package com.yachay.app.domain

/**
 * Formatos que o Yachay sabe ler. Para suportar um novo formato:
 * 1. adicione a entrada aqui (MIME + extensões),
 * 2. trate-o em [com.yachay.app.data.importer.BookImporter] (metadados/capa),
 * 3. crie um leitor e despache para ele em `ui/reader/BookOpener.kt`.
 */
enum class BookFormat(
    val mimeTypes: Set<String>,
    val extensions: Set<String>,
    /** Livro de páginas fixas (abre no leitor de páginas, com `totalPages` e "Página X de Y"). */
    val isPaged: Boolean,
) {
    PDF(setOf("application/pdf"), setOf("pdf"), isPaged = true),
    EPUB(setOf("application/epub+zip"), setOf("epub"), isPaged = false),
    CBZ(setOf("application/vnd.comicbook+zip", "application/x-cbz"), setOf("cbz"), isPaged = true);

    companion object {
        /**
         * MIME types usados no seletor de arquivos do sistema. Muitos provedores não conhecem o
         * MIME do CBZ e o anunciam como ZIP ou binário genérico; a extensão confirma na importação.
         */
        val pickerMimeTypes: Array<String> =
            (entries.flatMap { it.mimeTypes } + listOf("application/zip", "application/octet-stream")).toTypedArray()

        /**
         * Detecta o formato pelo MIME informado pelo provider ou, como fallback,
         * pela extensão do nome do arquivo (alguns provedores devolvem `application/octet-stream`).
         */
        fun detect(mimeType: String?, fileName: String?): BookFormat? {
            val mime = mimeType?.lowercase()
            entries.firstOrNull { mime in it.mimeTypes }?.let { return it }
            // ZIP genérico ou binário: decide pela extensão (e nada de importar um .zip qualquer).
            val ext = fileName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
            return entries.firstOrNull { ext in it.extensions }
        }
    }
}

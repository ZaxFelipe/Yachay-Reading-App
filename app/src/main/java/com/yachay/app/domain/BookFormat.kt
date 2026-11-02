package com.yachay.app.domain

/**
 * Formatos que o Yachay sabe ler. Para suportar um novo formato:
 * 1. adicione a entrada aqui (MIME + extensões),
 * 2. trate-o em [com.yachay.app.data.importer.BookImporter] (metadados/capa),
 * 3. crie um leitor e despache para ele em `ui/reader/BookOpener.kt`.
 */
enum class BookFormat(val mimeTypes: Set<String>, val extensions: Set<String>) {
    PDF(setOf("application/pdf"), setOf("pdf")),
    EPUB(setOf("application/epub+zip"), setOf("epub"));

    companion object {
        /** MIME types usados no seletor de arquivos do sistema. */
        val pickerMimeTypes: Array<String> = entries.flatMap { it.mimeTypes }.toTypedArray()

        /**
         * Detecta o formato pelo MIME informado pelo provider ou, como fallback,
         * pela extensão do nome do arquivo (alguns provedores devolvem `application/octet-stream`).
         */
        fun detect(mimeType: String?, fileName: String?): BookFormat? {
            val mime = mimeType?.lowercase()
            entries.firstOrNull { mime in it.mimeTypes }?.let { return it }
            val ext = fileName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
            return entries.firstOrNull { ext in it.extensions }
        }
    }
}

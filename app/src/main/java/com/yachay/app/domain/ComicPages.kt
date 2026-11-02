package com.yachay.app.domain

/** Regras do CBZ: quais entradas do ZIP são páginas e em que ordem. */
object ComicPages {
    private val ImageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

    /** Só imagens; ignora pastas de sistema (`__MACOSX/`), arquivos ocultos e metadados. */
    fun isPage(path: String): Boolean {
        val normalized = path.replace('\\', '/').trimStart('/')
        if (normalized.split('/').any { it.startsWith("__MACOSX") || it.startsWith(".") }) return false
        return normalized.substringAfterLast('.', "").lowercase() in ImageExtensions
    }

    /** Páginas do arquivo, na ordem natural dos nomes. */
    fun pages(paths: Iterable<String>): List<String> = paths.filter(::isPage).sortedWith(NaturalOrder)

    /**
     * Ordem natural: blocos de dígitos comparam como números ("page2" antes de "page10"),
     * o resto sem diferenciar maiúsculas. Empate exato cai na comparação simples.
     */
    val NaturalOrder: Comparator<String> = Comparator { a, b ->
        val ca = chunks(a)
        val cb = chunks(b)
        for (i in 0 until minOf(ca.size, cb.size)) {
            val x = ca[i]
            val y = cb[i]
            val bothNumbers = x[0].isDigit() && y[0].isDigit()
            val result = if (bothNumbers) {
                val nx = x.trimStart('0')
                val ny = y.trimStart('0')
                // Sem converter para Long: números enormes não estouram.
                if (nx.length != ny.length) nx.length - ny.length else nx.compareTo(ny)
            } else {
                x.compareTo(y, ignoreCase = true)
            }
            if (result != 0) return@Comparator result
        }
        if (ca.size != cb.size) ca.size - cb.size else a.compareTo(b)
    }

    private fun chunks(text: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val digit = text[i].isDigit()
            var j = i
            while (j < text.length && text[j].isDigit() == digit) j++
            out += text.substring(i, j)
            i = j
        }
        return out
    }
}

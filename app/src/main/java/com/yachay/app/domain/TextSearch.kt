package com.yachay.app.domain

import java.text.Normalizer

/** Comparação de texto para buscas: sem diferenciar maiúsculas nem acentos ("Ação" = "acao"). */
object TextSearch {
    private val Marks = Regex("\\p{M}+")

    /** Decompõe (NFD), tira as marcas e põe em minúsculas. */
    fun normalize(text: String): String =
        Marks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()

    fun contains(text: String?, query: String): Boolean {
        val needle = normalize(query.trim())
        return needle.isEmpty() || (text != null && normalize(text).contains(needle))
    }

    /**
     * Texto normalizado e, para cada caractere dele, o índice no original. Serve para achar o
     * termo no texto normalizado e pintar o trecho certo no texto como ele é.
     */
    private fun normalizeWithMap(text: String): Pair<String, IntArray> {
        val out = StringBuilder()
        val map = ArrayList<Int>(text.length)
        text.forEachIndexed { index, char ->
            normalize(char.toString()).forEach {
                out.append(it)
                map.add(index)
            }
        }
        return out.toString() to map.toIntArray()
    }

    /** Intervalos (no texto original) de cada ocorrência de [query], ignorando acentos e maiúsculas. */
    fun matches(text: String, query: String): List<IntRange> {
        val needle = normalize(query.trim())
        if (needle.isEmpty()) return emptyList()
        val (haystack, map) = normalizeWithMap(text)
        val ranges = mutableListOf<IntRange>()
        var from = haystack.indexOf(needle)
        while (from >= 0) {
            ranges += map[from]..map[from + needle.length - 1]
            from = haystack.indexOf(needle, from + needle.length)
        }
        return ranges
    }

    /**
     * Trecho de contexto em volta de [match]: até [radius] caracteres de cada lado, cortado em
     * limite de palavra, com reticências onde o texto continua. Devolve o trecho e o intervalo
     * do termo dentro dele.
     */
    fun snippet(text: String, match: IntRange, radius: Int = 60): Pair<String, IntRange> {
        var start = (match.first - radius).coerceAtLeast(0)
        var end = (match.last + 1 + radius).coerceAtMost(text.length)
        // Não corta palavra no meio: avança/recua até um espaço.
        if (start > 0) {
            val space = text.indexOf(' ', start)
            start = if (space in start until match.first) space + 1 else start
        }
        if (end < text.length) {
            val space = text.lastIndexOf(' ', end - 1)
            end = if (space > match.last) space else end
        }
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        val body = text.substring(start, end).replace(Whitespace, " ")
        // Espaços colapsados antes do termo mudam o deslocamento: recalcula no texto colapsado.
        val before = text.substring(start, match.first).replace(Whitespace, " ")
        val termLength = text.substring(match.first, match.last + 1).replace(Whitespace, " ").length
        val offset = prefix.length + before.length
        return (prefix + body + suffix) to (offset until offset + termLength)
    }

    private val Whitespace = Regex("\\s+")
}

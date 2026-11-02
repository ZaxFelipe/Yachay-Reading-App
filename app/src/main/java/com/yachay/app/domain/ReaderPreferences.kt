package com.yachay.app.domain

import kotlin.math.roundToInt

/** Tudo o que o leitor EPUB aplica de uma vez: tema das páginas e o texto. */
data class ReaderPreferences(
    val theme: ReadingTheme = ReadingTheme.Default,
    /** 1f = 100% (tamanho da editora). */
    val fontScale: Float = FontScale.DEFAULT,
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val font: ReaderFont = ReaderFont.PUBLISHER,
    val margins: PageMargins = PageMargins.NORMAL,
    val textAlign: TextAlignment = TextAlignment.PUBLISHER,
) {
    /**
     * Entrelinha e alinhamento só têm efeito no Readium 3.0.3 com os estilos da editora
     * desligados; fonte e margem funcionam com eles.
     */
    val overridesPublisherStyles: Boolean
        get() = lineSpacing != LineSpacing.NORMAL || textAlign != TextAlignment.PUBLISHER
}

/** Família de fonte do EPUB. [PUBLISHER] = a do livro. */
enum class ReaderFont {
    PUBLISHER,
    SERIF,
    SANS_SERIF,
    /** OpenDyslexic, embutida e declarada pelo próprio Readium. */
    LEGIBLE;

    companion object {
        fun fromName(name: String?): ReaderFont = entries.firstOrNull { it.name == name } ?: PUBLISHER
    }
}

/** Margem lateral, em múltiplos da margem padrão do Readium ([NORMAL] = a dele). */
enum class PageMargins(val factor: Double?) {
    NARROW(0.5),
    NORMAL(null),
    WIDE(2.0);

    companion object {
        fun fromName(name: String?): PageMargins = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

enum class TextAlignment {
    PUBLISHER,
    LEFT,
    JUSTIFY;

    companion object {
        fun fromName(name: String?): TextAlignment = entries.firstOrNull { it.name == name } ?: PUBLISHER
    }
}

/** Tamanho da fonte do EPUB: de 70% a 250%, em passos de 10%. */
object FontScale {
    const val MIN = 0.7f
    const val MAX = 2.5f
    const val DEFAULT = 1f

    /** Trabalha em décimos inteiros para não acumular erro de float (1.0999999). */
    fun next(current: Float, step: Int): Float {
        val tenths = (current * 10).roundToInt() + step
        return tenths.coerceIn(tenths(MIN), tenths(MAX)) / 10f
    }

    fun canStep(current: Float, step: Int): Boolean = next(current, step) != current

    /** Valor gravado inválido (ausente, NaN, fora da faixa) volta ao padrão. */
    fun sanitize(stored: Float?): Float =
        stored?.takeIf { !it.isNaN() && it in MIN..MAX } ?: DEFAULT

    fun percent(scale: Float): Int = (scale * 100).roundToInt()

    private fun tenths(value: Float): Int = (value * 10).roundToInt()
}

/**
 * Espaçamento entre linhas. [NORMAL] mantém o do livro; os outros só têm efeito no Readium
 * desligando os estilos da editora (ver `toEpubPreferences`).
 */
enum class LineSpacing(val lineHeight: Double?) {
    COMPACT(1.2),
    NORMAL(null),
    WIDE(1.8);

    companion object {
        fun fromName(name: String?): LineSpacing = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

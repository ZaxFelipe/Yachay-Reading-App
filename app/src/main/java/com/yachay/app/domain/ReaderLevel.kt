package com.yachay.app.domain

/**
 * Nível de leitor: cada meta diária batida rende [ReaderLevel.XP_PER_GOAL] de XP, e cada nível
 * desbloqueia um avatar (um por nível, de 1 a [ReaderLevel.MAX_LEVEL]).
 */
object ReaderLevel {
    const val MAX_LEVEL = 16
    const val XP_PER_GOAL = 100

    /**
     * XP acumulado para chegar ao nível [level]. Subir do nível L para o L+1 custa L metas:
     * os primeiros avatares chegam em dias, o último em ~120 metas (uns quatro meses).
     */
    fun xpFor(level: Int): Int {
        val l = level.coerceIn(1, MAX_LEVEL)
        return XP_PER_GOAL * (l - 1) * l / 2
    }

    fun levelFor(xp: Int): Int = (MAX_LEVEL downTo 1).first { xpFor(it) <= xp }
}

/** Números do perfil, somando todos os livros e dias. */
data class ReaderStats(
    /** Páginas contadas pela meta (cada página uma vez por dia). */
    val totalPages: Int = 0,
    val goalsMet: Int = 0,
    /** Avatar escolhido na galeria; null = o do nível atual. */
    val chosenAvatar: Int? = null,
) {
    val xp: Int get() = goalsMet * ReaderLevel.XP_PER_GOAL
    val level: Int get() = ReaderLevel.levelFor(xp)
    val isMaxLevel: Boolean get() = level >= ReaderLevel.MAX_LEVEL

    /** Foto de perfil: a escolhida, se já desbloqueada; senão a do nível. */
    val avatar: Int get() = chosenAvatar?.takeIf { it in 1..level } ?: level

    /** XP ganho dentro do nível atual e o necessário para o próximo (0 no nível máximo). */
    val xpInLevel: Int get() = xp - ReaderLevel.xpFor(level)
    val xpForNextLevel: Int get() = if (isMaxLevel) 0 else ReaderLevel.xpFor(level + 1) - ReaderLevel.xpFor(level)

    /** Preenchimento da barra de XP. */
    val levelFraction: Float get() = if (isMaxLevel) 1f else xpInLevel.toFloat() / xpForNextLevel

    /** Metas que faltam para o próximo avatar. */
    val goalsToNextLevel: Int
        get() = if (isMaxLevel) 0 else (xpForNextLevel - xpInLevel + ReaderLevel.XP_PER_GOAL - 1) / ReaderLevel.XP_PER_GOAL
}

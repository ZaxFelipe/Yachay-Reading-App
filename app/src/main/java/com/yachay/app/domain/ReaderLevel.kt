package com.yachay.app.domain

/**
 * Nível de leitor: cada meta diária batida rende XP proporcional ao alvo ([DailyGoal.xpFor];
 * a meta padrão vale [ReaderLevel.XP_PER_GOAL]), e cada nível desbloqueia um avatar
 * (um por nível, de 1 a [ReaderLevel.MAX_LEVEL]).
 */
object ReaderLevel {
    const val MAX_LEVEL = 16
    /** XP da meta padrão (10 páginas); a curva de níveis é medida nessa unidade. */
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
    /** XP guardado (metas proporcionais ao alvo + bônus de sequência); sem ele, o da meta padrão. */
    val xp: Int = goalsMet * ReaderLevel.XP_PER_GOAL,
    /** Avatar escolhido na galeria; null = o do nível atual. */
    val chosenAvatar: Int? = null,
    /** Alvo atual da meta: define quantas metas faltam para o próximo nível. */
    val goalTarget: Int = DailyGoal.DEFAULT_TARGET,
    /** Dias seguidos batendo a meta (já considerando hoje) e o recorde. */
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    val level: Int get() = ReaderLevel.levelFor(xp)
    val isMaxLevel: Boolean get() = level >= ReaderLevel.MAX_LEVEL

    /** Foto de perfil: a escolhida, se já desbloqueada; senão a do nível. */
    val avatar: Int get() = chosenAvatar?.takeIf { it in 1..level } ?: level

    /** XP ganho dentro do nível atual e o necessário para o próximo (0 no nível máximo). */
    val xpInLevel: Int get() = xp - ReaderLevel.xpFor(level)
    val xpForNextLevel: Int get() = if (isMaxLevel) 0 else ReaderLevel.xpFor(level + 1) - ReaderLevel.xpFor(level)

    /** Preenchimento da barra de XP. */
    val levelFraction: Float get() = if (isMaxLevel) 1f else xpInLevel.toFloat() / xpForNextLevel

    /** Metas (do alvo atual) que faltam para o próximo avatar. */
    val goalsToNextLevel: Int
        get() {
            if (isMaxLevel) return 0
            val perGoal = DailyGoal.xpFor(goalTarget)
            return (xpForNextLevel - xpInLevel + perGoal - 1) / perGoal
        }
}

package com.yachay.app.domain

import java.time.LocalDate

/**
 * Sequência de dias seguidos batendo a meta. Guarda o último dia batido e a contagem até ele;
 * o valor mostrado depende de hoje ([current]), então não precisa abrir o app para "zerar".
 */
object Streak {
    /** A cada tantos dias seguidos, XP extra (fixo: não depende do tamanho da meta). */
    const val BONUS_EVERY = 7
    const val BONUS_XP = 100

    /** Contagem depois de bater a meta [today]. Bater de novo no mesmo dia não soma. */
    fun onGoalMet(lastDay: LocalDate?, count: Int, today: LocalDate): Int = when (lastDay) {
        today -> count
        today.minusDays(1) -> count + 1
        else -> 1
    }

    /** Sequência mostrada: hoje sem meta e ontem batida ainda vale (o dia não acabou). */
    fun current(lastDay: LocalDate?, count: Int, today: LocalDate): Int =
        if (lastDay == today || lastDay == today.minusDays(1)) count else 0

    fun isBonus(count: Int): Boolean = count > 0 && count % BONUS_EVERY == 0
}

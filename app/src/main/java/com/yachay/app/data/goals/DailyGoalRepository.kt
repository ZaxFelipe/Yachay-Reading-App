package com.yachay.app.data.goals

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yachay.app.domain.DailyGoal
import com.yachay.app.domain.DailyGoalProgress
import com.yachay.app.domain.ReaderLevel
import com.yachay.app.domain.ReaderStats
import com.yachay.app.domain.ReadingDay
import com.yachay.app.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dailyGoalStore: DataStore<Preferences> by preferencesDataStore(name = "daily_goal")

/** Resultado de registrar uma página: o andamento novo e qual marco ele acabou de cruzar. */
data class PageRecorded(
    val progress: DailyGoalProgress,
    val reachedAlmostThere: Boolean,
    val reachedTarget: Boolean,
    /** Nível alcançado com a meta desta página (avatar novo), ou null. */
    val unlockedLevel: Int? = null,
    /** Sequência que acabou de chegar a um múltiplo de 7 (bônus de XP), ou null. */
    val streakBonus: Int? = null,
) {
    /** Cruzou um marco que merece aviso ("quase lá" ou meta batida). */
    val shouldNotify: Boolean get() = reachedAlmostThere || reachedTarget
}

/** Histórico por dia (calendário do Perfil). Fica fora do DataStore: ver [DailyGoalRepository.recordPage]. */
fun interface ReadingHistory {
    suspend fun record(date: LocalDate, pages: Int, goalMet: Boolean)
}

/**
 * Páginas lidas hoje (DataStore: só o dia atual), o alvo escolhido, os totais do perfil
 * (páginas, metas, XP) e a sequência de dias. Recebe o [DataStore] e o relógio para os testes
 * usarem arquivo temporário e trocar de dia.
 */
class DailyGoalRepository(
    private val store: DataStore<Preferences>,
    private val history: ReadingHistory = ReadingHistory { _, _, _ -> },
    private val today: () -> LocalDate = LocalDate::now,
) {

    /** Reavalia o dia a cada emissão; virar a meia-noite sem ler nada só aparece na próxima leitura. */
    val progress: Flow<DailyGoalProgress> = store.data
        .map { DailyGoalProgress(DailyGoal.pagesOn(it.readingDay(), today()), DailyGoal.sanitizeTarget(it[TARGET])) }
        .distinctUntilChanged()

    val target: Flow<Int> = store.data.map { DailyGoal.sanitizeTarget(it[TARGET]) }.distinctUntilChanged()

    val stats: Flow<ReaderStats> = store.data
        .map { prefs ->
            ReaderStats(
                totalPages = prefs[TOTAL_PAGES] ?: 0,
                goalsMet = prefs[GOALS_MET] ?: 0,
                xp = prefs.xp(),
                chosenAvatar = prefs[AVATAR],
                goalTarget = DailyGoal.sanitizeTarget(prefs[TARGET]),
                streak = Streak.current(prefs.streakLastDay(), prefs.streakCount(), today()),
                bestStreak = prefs.streakBest(),
            )
        }
        .distinctUntilChanged()

    /** Usa [level] como foto de perfil (avatares bloqueados são ignorados em [ReaderStats.avatar]). */
    suspend fun chooseAvatar(level: Int) {
        store.edit { it[AVATAR] = level }
    }

    /**
     * Vale na hora. Se as páginas de hoje já passam do alvo novo, a meta conta na próxima página
     * registrada; quem já bateu a meta hoje não ganha XP de novo ([GOAL_MET_DAY]).
     */
    suspend fun setTarget(target: Int) {
        store.edit { it[TARGET] = DailyGoal.sanitizeTarget(target) }
    }

    /** [page] vem de [DailyGoal.pdfPage] ou [DailyGoal.epubPage]. */
    suspend fun recordPage(bookId: Long, page: String): PageRecorded {
        var before = 0
        var after = 0
        var target = DailyGoal.DEFAULT_TARGET
        var reached = false
        var xpBefore = 0
        var xpAfter = 0
        var streakBonus: Int? = null
        var date = today()
        // Dia, totais, XP e sequência na mesma transação: uma página nunca conta para um e não para o outro.
        store.edit { prefs ->
            date = today()
            target = DailyGoal.sanitizeTarget(prefs[TARGET])
            // Congela a regra da 0.3.0 antes de somar páginas novas ("" = nenhum dia batido).
            if (prefs[GOAL_MET_DAY] == null) prefs[GOAL_MET_DAY] = prefs.goalMetDay()?.toString().orEmpty()
            val stored = prefs.readingDay()
            val metToday = prefs.goalMetDay() == date
            before = DailyGoal.pagesOn(stored, date)
            val day = DailyGoal.record(stored, date, DailyGoal.pageKey(bookId, page))
            after = day.pages.size
            prefs[DAY] = day.date.toString()
            prefs[PAGES] = day.pages
            // Sem total salvo (versão anterior), começa pelas páginas de hoje.
            prefs[TOTAL_PAGES] = (prefs[TOTAL_PAGES] ?: before) + (after - before)
            xpBefore = prefs.xp()
            xpAfter = xpBefore
            reached = !metToday && after >= target
            if (reached) {
                // Sequência antes de gravar o dia batido (sem chaves próprias ela deriva dele).
                val streak = Streak.onGoalMet(prefs.streakLastDay(), prefs.streakCount(), date)
                prefs[GOALS_MET] = (prefs[GOALS_MET] ?: 0) + 1
                prefs[GOAL_MET_DAY] = date.toString()
                prefs[STREAK_LAST_DAY] = date.toString()
                prefs[STREAK_COUNT] = streak
                prefs[STREAK_BEST] = maxOf(prefs.streakBest(), streak)
                xpAfter += DailyGoal.xpFor(target)
                if (Streak.isBonus(streak)) {
                    xpAfter += Streak.BONUS_XP
                    streakBonus = streak
                }
                // Avatar novo desbloqueado vira a foto, mesmo que outro tenha sido escolhido.
                if (ReaderLevel.levelFor(xpAfter) > ReaderLevel.levelFor(xpBefore)) prefs.remove(AVATAR)
            }
            prefs[XP] = xpAfter
        }
        // Transação separada (Room): grava o valor absoluto do dia, então a próxima página corrige uma falha.
        runCatching { history.record(date, after, reached || after >= target) }
        val levelBefore = ReaderLevel.levelFor(xpBefore)
        val levelAfter = ReaderLevel.levelFor(xpAfter)
        return PageRecorded(
            progress = DailyGoalProgress(after, target),
            reachedAlmostThere = DailyGoal.crossesAlmostThere(before, after, target),
            reachedTarget = reached,
            unlockedLevel = levelAfter.takeIf { it > levelBefore },
            streakBonus = streakBonus,
        )
    }

    private fun Preferences.readingDay(): ReadingDay? {
        val date = this[DAY].toDate() ?: return null
        return ReadingDay(date, this[PAGES].orEmpty())
    }

    /** XP salvo; quem vem da 0.3.0 (sem a chave) tinha 100 XP por meta. */
    private fun Preferences.xp(): Int = this[XP] ?: ((this[GOALS_MET] ?: 0) * ReaderLevel.XP_PER_GOAL)

    /**
     * Último dia com a meta batida. A 0.3.0 não guardava: se o dia salvo já tinha a meta fixa
     * de 10 páginas, ela foi paga naquele dia (vale só até a primeira página desta versão).
     */
    private fun Preferences.goalMetDay(): LocalDate? {
        this[GOAL_MET_DAY]?.let { return it.toDate() }
        val legacy = readingDay() ?: return null
        return legacy.date.takeIf { legacy.pages.size >= DailyGoal.DEFAULT_TARGET }
    }

    /** Sem chaves da sequência (versão anterior): começa em 1 se a meta do dia salvo foi batida. */
    private fun Preferences.streakLastDay(): LocalDate? = this[STREAK_LAST_DAY]?.toDate() ?: goalMetDay()

    private fun Preferences.streakCount(): Int = this[STREAK_COUNT] ?: if (goalMetDay() != null) 1 else 0

    private fun Preferences.streakBest(): Int = this[STREAK_BEST] ?: streakCount()

    private fun String?.toDate(): LocalDate? = this?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private companion object {
        val DAY = stringPreferencesKey("day")
        val PAGES = stringSetPreferencesKey("pages")
        val TOTAL_PAGES = intPreferencesKey("total_pages")
        val GOALS_MET = intPreferencesKey("goals_met")
        val AVATAR = intPreferencesKey("avatar")
        val TARGET = intPreferencesKey("target")
        val XP = intPreferencesKey("xp")
        val GOAL_MET_DAY = stringPreferencesKey("goal_met_day")
        val STREAK_LAST_DAY = stringPreferencesKey("streak_last_day")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val STREAK_BEST = intPreferencesKey("streak_best")
    }
}

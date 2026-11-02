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
import com.yachay.app.domain.ReaderStats
import com.yachay.app.domain.ReadingDay
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
) {
    /** Cruzou um marco que merece aviso ("quase lá" ou meta batida). */
    val shouldNotify: Boolean get() = reachedAlmostThere || reachedTarget
}

/**
 * Páginas lidas hoje (DataStore: só o dia atual, sem histórico) e os totais do perfil
 * (páginas e metas batidas, de onde sai o XP). Recebe o [DataStore] e o
 * relógio para os testes usarem arquivo temporário e trocar de dia.
 */
class DailyGoalRepository(
    private val store: DataStore<Preferences>,
    private val today: () -> LocalDate = LocalDate::now,
) {

    /** Reavalia o dia a cada emissão; virar a meia-noite sem ler nada só aparece na próxima leitura. */
    val progress: Flow<DailyGoalProgress> = store.data
        .map { DailyGoalProgress(DailyGoal.pagesOn(it.readingDay(), today())) }
        .distinctUntilChanged()

    val stats: Flow<ReaderStats> = store.data
        .map { ReaderStats(totalPages = it[TOTAL_PAGES] ?: 0, goalsMet = it[GOALS_MET] ?: 0, chosenAvatar = it[AVATAR]) }
        .distinctUntilChanged()

    /** Usa [level] como foto de perfil (avatares bloqueados são ignorados em [ReaderStats.avatar]). */
    suspend fun chooseAvatar(level: Int) {
        store.edit { it[AVATAR] = level }
    }

    /** [page] vem de [DailyGoal.pdfPage] ou [DailyGoal.epubPage]. */
    suspend fun recordPage(bookId: Long, page: String): PageRecorded {
        var before = 0
        var after = 0
        var goalsBefore = 0
        val target = DailyGoal.DEFAULT_TARGET
        // Dia, totais e XP na mesma transação: uma página nunca conta para um e não para o outro.
        store.edit { prefs ->
            val date = today()
            val stored = prefs.readingDay()
            before = DailyGoal.pagesOn(stored, date)
            val day = DailyGoal.record(stored, date, DailyGoal.pageKey(bookId, page))
            after = day.pages.size
            prefs[DAY] = day.date.toString()
            prefs[PAGES] = day.pages
            // Sem total salvo (versão anterior), começa pelas páginas de hoje.
            prefs[TOTAL_PAGES] = (prefs[TOTAL_PAGES] ?: before) + (after - before)
            goalsBefore = prefs[GOALS_MET] ?: 0
            if (DailyGoal.reachesTarget(before, after, target)) {
                prefs[GOALS_MET] = goalsBefore + 1
                // Avatar novo desbloqueado vira a foto, mesmo que outro tenha sido escolhido.
                if (ReaderStats(goalsMet = goalsBefore + 1).level > ReaderStats(goalsMet = goalsBefore).level) prefs.remove(AVATAR)
            }
        }
        val reachedTarget = DailyGoal.reachesTarget(before, after, target)
        val levelBefore = ReaderStats(goalsMet = goalsBefore).level
        val levelAfter = ReaderStats(goalsMet = goalsBefore + if (reachedTarget) 1 else 0).level
        return PageRecorded(
            progress = DailyGoalProgress(after, target),
            reachedAlmostThere = DailyGoal.crossesAlmostThere(before, after, target),
            reachedTarget = reachedTarget,
            unlockedLevel = levelAfter.takeIf { it > levelBefore },
        )
    }

    private fun Preferences.readingDay(): ReadingDay? {
        val date = this[DAY]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return ReadingDay(date, this[PAGES].orEmpty())
    }

    private companion object {
        val DAY = stringPreferencesKey("day")
        val PAGES = stringSetPreferencesKey("pages")
        val TOTAL_PAGES = intPreferencesKey("total_pages")
        val GOALS_MET = intPreferencesKey("goals_met")
        val AVATAR = intPreferencesKey("avatar")
    }
}

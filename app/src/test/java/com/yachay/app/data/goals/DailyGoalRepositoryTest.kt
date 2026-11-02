package com.yachay.app.data.goals

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.yachay.app.domain.DailyGoalProgress
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class DailyGoalRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var date = LocalDate.of(2026, 11, 2)

    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope) {
            folder.newFile("daily_goal.preferences_pb").also { it.delete() }
        }
    }

    /** Dias gravados no histórico (o Room, nos testes, é um mapa). */
    private val history = mutableMapOf<LocalDate, Pair<Int, Boolean>>()

    private val repository by lazy {
        DailyGoalRepository(store, history = { day, pages, met -> history[day] = pages to met }) { date }
    }

    /** Lê uma meta inteira de [pages] páginas novas. */
    private suspend fun readPages(pages: Int, prefix: String = "d$date-"): PageRecorded =
        (1..pages).map { repository.recordPage(bookId = 1, page = "$prefix$it") }.last()

    private suspend fun nextDay() {
        date = date.plusDays(1)
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `counts distinct pages and fires almost there once at 6 of 10`() = runBlocking {
        val crossings = (1..8).map { repository.recordPage(bookId = 1, page = "p$it").reachedAlmostThere }
        assertEquals(listOf(false, false, false, false, false, true, false, false), crossings)
        // Reler uma página já contada não soma.
        assertEquals(8, repository.recordPage(bookId = 1, page = "p3").progress.pagesRead)
        assertEquals(8, repository.progress.first().pagesRead)
    }

    @Test
    fun `reaching the target fires once and only on the tenth page`() = runBlocking {
        val reached = (1..12).map { repository.recordPage(bookId = 1, page = "p$it").reachedTarget }
        assertEquals((1..12).map { it == 10 }, reached)
        assertTrue(repository.progress.first().isDone)
    }

    @Test
    fun `next day starts from zero`() = runBlocking {
        repeat(4) { repository.recordPage(bookId = 1, page = "p$it") }
        date = date.plusDays(1)
        assertEquals(0, repository.progress.first().pagesRead)
        val recorded = repository.recordPage(bookId = 1, page = "p0")
        assertEquals(1, recorded.progress.pagesRead)
        assertFalse(recorded.reachedAlmostThere)
        assertTrue(recorded.progress.fraction > 0f)
    }

    @Test
    fun `totals add up across days and each goal gives xp`() = runBlocking {
        val first = (1..10).map { repository.recordPage(bookId = 1, page = "p$it") }
        // A 1ª meta leva ao nível 2: avatar novo só na página que bate a meta.
        assertEquals((1..10).map { if (it == 10) 2 else null }, first.map { it.unlockedLevel })
        repository.recordPage(bookId = 1, page = "p1") // repetida no mesmo dia: não soma
        date = date.plusDays(1)
        repository.recordPage(bookId = 1, page = "p1") // mesma página em outro dia: soma
        val stats = repository.stats.first()
        assertEquals(11, stats.totalPages)
        assertEquals(1, stats.goalsMet)
        assertEquals(100, stats.xp)
    }

    @Test
    fun `second goal adds xp without a new avatar`() = runBlocking {
        repeat(10) { repository.recordPage(bookId = 1, page = "a$it") }
        date = date.plusDays(1)
        val second = (0 until 10).map { repository.recordPage(bookId = 2, page = "b$it") }.last()
        assertTrue(second.reachedTarget)
        assertEquals(null, second.unlockedLevel) // nível 3 pede 3 metas
        assertEquals(2, repository.stats.first().level)
    }

    @Test
    fun `chosen avatar sticks until a new one is unlocked`() = runBlocking {
        repeat(10) { repository.recordPage(bookId = 1, page = "a$it") } // nível 2
        repository.chooseAvatar(1)
        assertEquals(1, repository.stats.first().avatar)
        repository.chooseAvatar(5) // bloqueado: continua no do nível
        assertEquals(2, repository.stats.first().avatar)
        repository.chooseAvatar(1)
        date = date.plusDays(1)
        repeat(10) { repository.recordPage(bookId = 1, page = "b$it") } // 2ª meta: ainda nível 2
        assertEquals(1, repository.stats.first().avatar)
        date = date.plusDays(1)
        repeat(10) { repository.recordPage(bookId = 1, page = "c$it") } // 3ª meta: nível 3
        assertEquals(3, repository.stats.first().avatar)
    }

    @Test
    fun `xp is proportional to the chosen target`() = runBlocking {
        repository.setTarget(20)
        assertTrue(readPages(20).reachedTarget)
        assertEquals(200, repository.stats.first().xp)
        nextDay()
        repository.setTarget(5)
        val five = (1..5).map { repository.recordPage(bookId = 1, page = "x$it") }
        assertEquals((1..5).map { it == 5 }, five.map { it.reachedTarget })
        assertEquals(250, repository.stats.first().xp)
        assertEquals(2, repository.stats.first().goalsMet)
    }

    @Test
    fun `lowering the target mid-day counts on the next page and pays only once`() = runBlocking {
        repository.setTarget(20)
        readPages(12)
        repository.setTarget(10) // 12 já passa de 10: sem disparar retroativamente
        assertEquals(0, repository.stats.first().goalsMet)
        assertTrue(repository.recordPage(bookId = 1, page = "extra").reachedTarget)
        assertEquals(100, repository.stats.first().xp)
        repository.setTarget(5) // baixar de novo e ler mais: nada de XP duplicado
        val again = repository.recordPage(bookId = 1, page = "extra2")
        assertFalse(again.reachedTarget)
        assertEquals(100, repository.stats.first().xp)
        assertEquals(1, repository.stats.first().goalsMet)
    }

    @Test
    fun `target change shows on progress right away`() = runBlocking {
        readPages(3)
        repository.setTarget(5)
        assertEquals(DailyGoalProgress(3, 5), repository.progress.first())
        repository.setTarget(7) // inválido: volta ao padrão
        assertEquals(10, repository.progress.first().target)
    }

    @Test
    fun `streak counts consecutive goals and resets after a gap`() = runBlocking {
        repeat(3) {
            readPages(10)
            nextDay()
        }
        // Hoje sem meta ainda: mostra a sequência de ontem.
        assertEquals(3, repository.stats.first().streak)
        nextDay() // pulou um dia
        assertEquals(0, repository.stats.first().streak)
        readPages(10)
        val stats = repository.stats.first()
        assertEquals(1, stats.streak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun `seventh day gives bonus xp once`() = runBlocking {
        val goals = (1..8).map {
            readPages(10).also { nextDay() }
        }
        assertEquals((1..8).map { if (it == 7) 7 else null }, goals.map { it.streakBonus })
        // 8 metas de 100 + 1 bônus de 100.
        assertEquals(900, repository.stats.first().xp)
    }

    @Test
    fun `bonus xp counts towards unlocking avatars`() = runBlocking {
        // 6 metas = 600 XP (nível 4). A 7ª soma 100 + 100 de bônus = 800: ainda nível 4 (nível 5 = 1000).
        repeat(6) { readPages(10).also { nextDay() } }
        assertEquals(4, repository.stats.first().level)
        repository.setTarget(30)
        // 7ª meta de 30 páginas: 300 + 100 de bônus = 1000 → nível 5 só por causa do bônus.
        val seventh = readPages(30)
        assertEquals(7, seventh.streakBonus)
        assertEquals(5, seventh.unlockedLevel)
        assertEquals(1000, repository.stats.first().xp)
    }

    @Test
    fun `upgrading from 0_3_0 keeps xp and starts the streak from today`() = runBlocking {
        // Estado gravado pela 0.3.0: meta de hoje batida, 4 metas no total.
        store.edit {
            it[stringPreferencesKey("day")] = date.toString()
            it[stringSetPreferencesKey("pages")] = (1..10).map { n -> "1:p$n" }.toSet()
            it[intPreferencesKey("goals_met")] = 4
            it[intPreferencesKey("total_pages")] = 40
        }
        val stats = repository.stats.first()
        assertEquals(400, stats.xp)
        assertEquals(3, stats.level)
        assertEquals(1, stats.streak)
        // A meta de hoje já foi paga na 0.3.0: ler mais não paga de novo.
        assertFalse(repository.recordPage(bookId = 1, page = "p11").reachedTarget)
        assertEquals(400, repository.stats.first().xp)
        nextDay()
        readPages(10)
        assertEquals(2, repository.stats.first().streak)
        assertEquals(500, repository.stats.first().xp)
    }

    @Test
    fun `upgrading without today's goal starts the streak at zero`() = runBlocking {
        store.edit {
            it[stringPreferencesKey("day")] = date.toString()
            it[stringSetPreferencesKey("pages")] = setOf("1:p1")
            it[intPreferencesKey("goals_met")] = 1
        }
        assertEquals(0, repository.stats.first().streak)
        repository.setTarget(20)
        readPages(15) // 16 páginas hoje: passa de 10 mas não de 20
        assertEquals(0, repository.stats.first().streak)
        assertTrue(readPages(4, prefix = "more").reachedTarget)
        assertEquals(1, repository.stats.first().streak)
    }

    @Test
    fun `history keeps the absolute pages of each day`() = runBlocking {
        readPages(4)
        assertEquals(4 to false, history[date])
        readPages(6, prefix = "b")
        assertEquals(10 to true, history[date])
        repository.recordPage(bookId = 1, page = "b1") // repetida: dia continua igual
        assertEquals(10 to true, history[date])
        val yesterday = date
        nextDay()
        readPages(2)
        assertEquals(10 to true, history[yesterday])
        assertEquals(2 to false, history[date])
    }
}

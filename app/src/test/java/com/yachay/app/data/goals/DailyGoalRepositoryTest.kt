package com.yachay.app.data.goals

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class DailyGoalRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var date = LocalDate.of(2026, 11, 2)

    private val repository by lazy {
        val store = PreferenceDataStoreFactory.create(scope = scope) {
            folder.newFile("daily_goal.preferences_pb").also { it.delete() }
        }
        DailyGoalRepository(store) { date }
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
}

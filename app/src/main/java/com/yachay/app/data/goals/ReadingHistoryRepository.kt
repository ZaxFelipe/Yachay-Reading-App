package com.yachay.app.data.goals

import com.yachay.app.data.db.ReadingDayDao
import com.yachay.app.data.db.ReadingDayEntity
import com.yachay.app.domain.CalendarDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Histórico de dias lidos (Room), gravado por [DailyGoalRepository.recordPage]. */
class ReadingHistoryRepository(private val dao: ReadingDayDao) {

    suspend fun record(date: LocalDate, pages: Int, goalMet: Boolean) {
        dao.upsert(ReadingDayEntity(date.toString(), pages, goalMet))
    }

    fun observe(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, CalendarDay>> =
        dao.observeBetween(from.toString(), to.toString()).map { days ->
            days.associate { LocalDate.parse(it.date) to CalendarDay(it.pages, it.goalMet) }
        }

    /** Primeiro dia com histórico (o dia em que esta versão começou a gravar), ou null. */
    val firstDay: Flow<LocalDate?> = dao.observeFirstDay().map { it?.let(LocalDate::parse) }
}

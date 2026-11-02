package com.yachay.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDayDao {

    /** Valor absoluto do dia (não incremento): se uma gravação falhar, a próxima corrige. */
    @Upsert
    suspend fun upsert(day: ReadingDayEntity)

    /** Datas ISO comparam como texto na ordem certa. */
    @Query("SELECT * FROM reading_days WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: String, to: String): Flow<List<ReadingDayEntity>>

    @Query("SELECT MIN(date) FROM reading_days")
    fun observeFirstDay(): Flow<String?>
}

package com.raghif.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BakeryDao {
    @Query("SELECT * FROM bakeries ORDER BY createdAt ASC") fun observeAll(): Flow<List<Bakery>>
    @Query("SELECT * FROM bakeries WHERE id=:id LIMIT 1") suspend fun get(id: Long): Bakery?
    @Query("SELECT COUNT(*) FROM bakeries WHERE normalizedNumber=:n AND id != :id") suspend fun numberExists(n: String, id: Long): Int
    @Query("SELECT COUNT(*) FROM bakeries WHERE normalizedName=:n AND id != :id") suspend fun nameExists(n: String, id: Long): Int
    @Insert suspend fun insert(item: Bakery): Long
    @Update suspend fun update(item: Bakery)
    @Delete suspend fun delete(item: Bakery)
}
@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_records WHERE bakeryId=:id AND date=:date LIMIT 1") suspend fun byDate(id: Long, date: String): DailyRecord?
    @Query("SELECT * FROM daily_records WHERE bakeryId=:id ORDER BY date DESC") suspend fun all(id: Long): List<DailyRecord>
    @Query("SELECT * FROM daily_records WHERE bakeryId=:id AND date BETWEEN :from AND :to ORDER BY date ASC") suspend fun between(id: Long, from: String, to: String): List<DailyRecord>
    @Query("SELECT * FROM daily_records WHERE bakeryId=:id ORDER BY date DESC") fun observe(id: Long): Flow<List<DailyRecord>>
    @Insert suspend fun insert(item: DailyRecord): Long
    @Update suspend fun update(item: DailyRecord)
    @Delete suspend fun delete(item: DailyRecord)
}

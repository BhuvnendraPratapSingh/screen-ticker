package com.socialwatch.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.socialwatch.app.data.db.entity.DailySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummary)

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun getSummariesForDate(date: String): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getSummariesForRange(startDate: String, endDate: String): Flow<List<DailySummary>>

    @Query("""
        SELECT COALESCE(SUM(totalDurationMs), 0) 
        FROM daily_summaries 
        WHERE packageName = :packageName AND date = :date
    """)
    suspend fun getTotalForPackageOnDate(packageName: String, date: String): Long

    @Query("""
        UPDATE daily_summaries 
        SET totalDurationMs = :totalMs 
        WHERE packageName = :packageName AND date = :date
    """)
    suspend fun updateTotal(packageName: String, date: String, totalMs: Long)

    @Query("SELECT COUNT(*) FROM daily_summaries WHERE packageName = :packageName AND date = :date")
    suspend fun exists(packageName: String, date: String): Int
}

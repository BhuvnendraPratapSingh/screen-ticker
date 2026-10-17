package com.socialwatch.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.socialwatch.app.data.db.entity.UsageSession
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: UsageSession): Long

    @Update
    suspend fun update(session: UsageSession)

    @Query("SELECT * FROM usage_sessions WHERE id = :id")
    suspend fun getById(id: Long): UsageSession?

    /** Get all sessions for a given date. */
    @Query("SELECT * FROM usage_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsForDate(date: String): Flow<List<UsageSession>>

    /** Get total duration per package for a given date. */
    @Query("""
        SELECT packageName, SUM(durationMs) as totalMs 
        FROM usage_sessions 
        WHERE date = :date 
        GROUP BY packageName
    """)
    fun getDailyTotals(date: String): Flow<List<PackageDuration>>

    /** Get total duration per package across a date range. */
    @Query("""
        SELECT packageName, SUM(durationMs) as totalMs 
        FROM usage_sessions 
        WHERE date BETWEEN :startDate AND :endDate 
        GROUP BY packageName
    """)
    fun getTotalsForRange(startDate: String, endDate: String): Flow<List<PackageDuration>>

    /** Get daily totals per package, grouped by date, for a date range (for charts). */
    @Query("""
        SELECT date, packageName, SUM(durationMs) as totalMs 
        FROM usage_sessions 
        WHERE date BETWEEN :startDate AND :endDate 
        GROUP BY date, packageName 
        ORDER BY date ASC
    """)
    fun getDailyBreakdown(startDate: String, endDate: String): Flow<List<DatePackageDuration>>

    /** Get hourly breakdown for a specific date (for daily chart). */
    @Query("""
        SELECT packageName, 
               CAST(strftime('%H', startTime / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour,
               SUM(durationMs) as totalMs
        FROM usage_sessions
        WHERE date = :date
        GROUP BY packageName, hour
        ORDER BY hour ASC
    """)
    fun getHourlyBreakdown(date: String): Flow<List<HourlyPackageDuration>>

    /** Get today's total for a specific package. */
    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM usage_sessions WHERE packageName = :packageName AND date = :date")
    suspend fun getTodayTotalForPackage(packageName: String, date: String): Long

    /** Close any open session (endTime = 0) for a package. */
    @Query("""
        UPDATE usage_sessions 
        SET endTime = :endTime, durationMs = :endTime - startTime 
        WHERE packageName = :packageName AND endTime = 0
    """)
    suspend fun closeOpenSessions(packageName: String, endTime: Long)

    /** Close all open sessions. */
    @Query("""
        UPDATE usage_sessions 
        SET endTime = :endTime, durationMs = :endTime - startTime 
        WHERE endTime = 0
    """)
    suspend fun closeAllOpenSessions(endTime: Long)
}

/** Data class for package → duration query results. */
data class PackageDuration(
    val packageName: String,
    val totalMs: Long
)

/** Data class for date → package → duration query results. */
data class DatePackageDuration(
    val date: String,
    val packageName: String,
    val totalMs: Long
)

/** Data class for hourly breakdown query results. */
data class HourlyPackageDuration(
    val packageName: String,
    val hour: Int,
    val totalMs: Long
)

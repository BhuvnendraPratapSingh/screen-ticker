package com.socialwatch.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aggregated daily usage per tracked app.
 * Updated periodically by the monitoring service.
 */
@Entity(
    tableName = "daily_summaries",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class DailySummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,

    /** Date string in "yyyy-MM-dd" format. */
    val date: String,

    /** Total usage time for this app on this date in milliseconds. */
    val totalDurationMs: Long = 0
)

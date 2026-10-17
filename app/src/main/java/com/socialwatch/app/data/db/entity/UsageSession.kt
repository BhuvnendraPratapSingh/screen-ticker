package com.socialwatch.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single continuous usage session of a tracked app.
 * A new session is created each time the user opens a tracked app.
 */
@Entity(tableName = "usage_sessions")
data class UsageSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,

    /** Epoch millis when the session started. */
    val startTime: Long,

    /** Epoch millis when the session ended. 0 if still active. */
    val endTime: Long = 0,

    /** Total duration of this session in milliseconds. */
    val durationMs: Long = 0,

    /** Date string in "yyyy-MM-dd" format for easy grouping. */
    val date: String
)

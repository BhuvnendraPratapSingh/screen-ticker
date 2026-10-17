package com.socialwatch.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a social media app that the user wants to track.
 */
@Entity(tableName = "tracked_apps")
data class TrackedApp(
    @PrimaryKey
    val packageName: String,

    val appName: String,

    /** Daily time limit in minutes. 0 means no limit. */
    val dailyLimitMinutes: Int = 0,

    /** Whether tracking is enabled for this app. */
    val isEnabled: Boolean = true
)

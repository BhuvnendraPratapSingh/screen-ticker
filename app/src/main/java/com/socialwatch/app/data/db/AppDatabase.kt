package com.socialwatch.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.socialwatch.app.data.db.dao.DailySummaryDao
import com.socialwatch.app.data.db.dao.TrackedAppDao
import com.socialwatch.app.data.db.dao.UsageSessionDao
import com.socialwatch.app.data.db.entity.DailySummary
import com.socialwatch.app.data.db.entity.TrackedApp
import com.socialwatch.app.data.db.entity.UsageSession

@Database(
    entities = [
        TrackedApp::class,
        UsageSession::class,
        DailySummary::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun dailySummaryDao(): DailySummaryDao
}

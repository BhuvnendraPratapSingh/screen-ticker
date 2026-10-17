package com.socialwatch.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.socialwatch.app.data.db.AppDatabase
import com.socialwatch.app.data.db.dao.DailySummaryDao
import com.socialwatch.app.data.db.dao.TrackedAppDao
import com.socialwatch.app.data.db.dao.UsageSessionDao
import com.socialwatch.app.data.db.entity.TrackedApp
import com.socialwatch.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "socialwatch_db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pre-populate default tracked apps
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val defaultApps = Constants.DEFAULT_TRACKED_APPS.map { (pkg, name) ->
                            TrackedApp(
                                packageName = pkg,
                                appName = name,
                                dailyLimitMinutes = 0,
                                isEnabled = true
                            )
                        }
                        // Use raw SQL since we don't have access to the DAO in the callback
                        defaultApps.forEach { app ->
                            db.execSQL(
                                """INSERT OR REPLACE INTO tracked_apps 
                                   (packageName, appName, dailyLimitMinutes, isEnabled) 
                                   VALUES (?, ?, ?, ?)""",
                                arrayOf(app.packageName, app.appName, app.dailyLimitMinutes, if (app.isEnabled) 1 else 0)
                            )
                        }
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTrackedAppDao(database: AppDatabase): TrackedAppDao {
        return database.trackedAppDao()
    }

    @Provides
    fun provideUsageSessionDao(database: AppDatabase): UsageSessionDao {
        return database.usageSessionDao()
    }

    @Provides
    fun provideDailySummaryDao(database: AppDatabase): DailySummaryDao {
        return database.dailySummaryDao()
    }
}

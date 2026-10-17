package com.socialwatch.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.socialwatch.app.data.db.entity.TrackedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {

    @Query("SELECT * FROM tracked_apps ORDER BY appName ASC")
    fun getAllTrackedApps(): Flow<List<TrackedApp>>

    @Query("SELECT * FROM tracked_apps WHERE isEnabled = 1")
    fun getEnabledTrackedApps(): Flow<List<TrackedApp>>

    @Query("SELECT * FROM tracked_apps WHERE isEnabled = 1")
    suspend fun getEnabledTrackedAppsList(): List<TrackedApp>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName")
    suspend fun getTrackedApp(packageName: String): TrackedApp?

    @Query("SELECT packageName FROM tracked_apps WHERE isEnabled = 1")
    suspend fun getEnabledPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: TrackedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<TrackedApp>)

    @Update
    suspend fun update(app: TrackedApp)

    @Delete
    suspend fun delete(app: TrackedApp)

    @Query("DELETE FROM tracked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE tracked_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE tracked_apps SET dailyLimitMinutes = :limitMinutes WHERE packageName = :packageName")
    suspend fun setDailyLimit(packageName: String, limitMinutes: Int)
}

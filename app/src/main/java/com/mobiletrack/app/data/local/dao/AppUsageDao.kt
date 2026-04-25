package com.mobiletrack.app.data.local.dao

import androidx.room.*
import com.mobiletrack.app.data.local.entity.AppUsageSession
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppUsageDao {

    @Query("SELECT * FROM app_usage_sessions WHERE date = :date ORDER BY totalMinutes DESC")
    abstract fun getUsageForDate(date: String): Flow<List<AppUsageSession>>

    @Query("SELECT * FROM app_usage_sessions WHERE date >= :fromDate ORDER BY date DESC, totalMinutes DESC")
    abstract fun getUsageFrom(fromDate: String): Flow<List<AppUsageSession>>

    @Query("SELECT * FROM app_usage_sessions WHERE packageName = :pkg AND date = :date LIMIT 1")
    abstract suspend fun getSessionForApp(pkg: String, date: String): AppUsageSession?

    @Query("SELECT SUM(totalMinutes) FROM app_usage_sessions WHERE date = :date")
    abstract fun getTotalMinutesForDate(date: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(session: AppUsageSession)

    @Query("UPDATE app_usage_sessions SET totalScrolls = totalScrolls + :count WHERE packageName = :pkg AND date = :date")
    abstract suspend fun incrementScrolls(pkg: String, date: String, count: Int = 1)

    @Query("SELECT totalScrolls FROM app_usage_sessions WHERE packageName = :pkg AND date = :date LIMIT 1")
    abstract suspend fun getScrollsForApp(pkg: String, date: String): Int?

    @Query("UPDATE app_usage_sessions SET totalMinutes = :minutes, appName = :appName, updatedAt = :updatedAt WHERE packageName = :pkg AND date = :date")
    abstract suspend fun updateUsageStats(pkg: String, appName: String, date: String, minutes: Int, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(session: AppUsageSession): Long

    @Query("UPDATE app_usage_sessions SET openCount = openCount + 1 WHERE packageName = :pkg AND date = :date")
    abstract suspend fun incrementOpenCount(pkg: String, date: String)

    @Query("DELETE FROM app_usage_sessions WHERE date < :beforeDate")
    abstract suspend fun deleteOlderThan(beforeDate: String)

    @Transaction
    open suspend fun upsertUsageStats(session: AppUsageSession) {
        val id = insertIfAbsent(session)
        if (id == -1L) {
            // Row already exists — update only time and name, leave openCount/scrolls untouched
            updateUsageStats(
                pkg = session.packageName,
                appName = session.appName,
                date = session.date,
                minutes = session.totalMinutes,
                updatedAt = session.updatedAt
            )
        }
    }
}

package com.mobiletrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import kotlinx.coroutines.flow.Flow

data class AppOpenCount(
    val packageName: String,
    val appName: String,
    val openCount: Int
)

data class HourlyCount(
    val hourOfDay: Int,
    val count: Int
)

@Dao
interface AppOpenEventDao {

    @Insert
    suspend fun insert(event: AppOpenEvent)

    @Query(
        "SELECT packageName, appName, COUNT(*) as openCount " +
        "FROM app_open_events " +
        "WHERE timestamp >= :fromMs " +
        "GROUP BY packageName " +
        "ORDER BY openCount DESC"
    )
    fun getOpenCountsSince(fromMs: Long): Flow<List<AppOpenCount>>

    @Query(
        "SELECT hourOfDay, COUNT(*) as count " +
        "FROM app_open_events " +
        "WHERE timestamp >= :fromMs " +
        "GROUP BY hourOfDay " +
        "ORDER BY hourOfDay"
    )
    fun getHourlyBreakdownSince(fromMs: Long): Flow<List<HourlyCount>>

    @Query("DELETE FROM app_open_events WHERE timestamp < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}

package com.mobiletrack.app.data.local.dao

import androidx.room.*
import com.mobiletrack.app.data.local.entity.UnlockEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockDao {

    @Insert
    suspend fun insert(event: UnlockEvent)

    @Query("SELECT COUNT(*) FROM unlock_events WHERE timestamp >= :fromMs")
    fun countSince(fromMs: Long): Flow<Int>

    @Query("SELECT * FROM unlock_events WHERE timestamp >= :fromMs ORDER BY timestamp DESC")
    fun getEventsSince(fromMs: Long): Flow<List<UnlockEvent>>

    @Query("DELETE FROM unlock_events WHERE timestamp < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}

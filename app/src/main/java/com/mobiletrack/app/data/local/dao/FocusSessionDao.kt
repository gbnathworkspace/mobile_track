package com.mobiletrack.app.data.local.dao

import androidx.room.*
import com.mobiletrack.app.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startHour ASC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1")
    fun getActiveSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSession)

    @Delete
    suspend fun delete(session: FocusSession)
}

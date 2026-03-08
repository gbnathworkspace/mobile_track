package com.mobiletrack.app.data.local.dao

import androidx.room.*
import com.mobiletrack.app.data.local.entity.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun getRuleForApp(pkg: String): AppRule?

    @Query("SELECT * FROM app_rules WHERE isBlocked = 1")
    fun getBlockedApps(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE dailyLimitMinutes > 0")
    fun getLimitedApps(): Flow<List<AppRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRule)

    @Delete
    suspend fun delete(rule: AppRule)

    @Query("DELETE FROM app_rules WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)
}

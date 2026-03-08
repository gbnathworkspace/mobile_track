package com.mobiletrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 0,   // 0 = no limit
    val isBlocked: Boolean = false,
    val blockedDuringFocus: Boolean = false
)

package com.mobiletrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage_sessions",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class AppUsageSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: String,           // yyyy-MM-dd
    val totalMinutes: Int,
    val openCount: Int,
    val totalScrolls: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

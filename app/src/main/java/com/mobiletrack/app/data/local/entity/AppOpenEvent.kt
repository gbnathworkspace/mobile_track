package com.mobiletrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "app_open_events")
data class AppOpenEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val unlockPurpose: String? = null
)

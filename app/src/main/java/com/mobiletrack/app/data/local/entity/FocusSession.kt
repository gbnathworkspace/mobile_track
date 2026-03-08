package com.mobiletrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val startHour: Int,       // 0–23
    val endHour: Int,         // 0–23
    val daysOfWeek: String,   // e.g. "MON,TUE,WED,THU,FRI"
    val isActive: Boolean = true
)

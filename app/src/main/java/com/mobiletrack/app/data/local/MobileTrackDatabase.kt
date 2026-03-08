package com.mobiletrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobiletrack.app.data.local.dao.*
import com.mobiletrack.app.data.local.entity.*

@Database(
    entities = [
        AppUsageSession::class,
        UnlockEvent::class,
        AppRule::class,
        FocusSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MobileTrackDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun unlockDao(): UnlockDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun focusSessionDao(): FocusSessionDao
}

package com.mobiletrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobiletrack.app.data.local.dao.*
import com.mobiletrack.app.data.local.entity.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_rules ADD COLUMN dailyScrollLimit INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE app_usage_sessions ADD COLUMN totalScrolls INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_open_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                packageName TEXT NOT NULL,
                appName TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                hourOfDay INTEGER NOT NULL,
                unlockPurpose TEXT
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        AppUsageSession::class,
        UnlockEvent::class,
        AppRule::class,
        FocusSession::class,
        AppOpenEvent::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MobileTrackDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun unlockDao(): UnlockDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun appOpenEventDao(): AppOpenEventDao
}

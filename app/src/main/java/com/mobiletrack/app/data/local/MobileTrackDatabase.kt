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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Deduplicate: keep the row with highest totalMinutes for each (packageName, date)
        db.execSQL(
            """
            DELETE FROM app_usage_sessions WHERE id NOT IN (
                SELECT MAX(id) FROM app_usage_sessions GROUP BY packageName, date
            )
            """.trimIndent()
        )
        // Add unique index to prevent future duplicates
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_app_usage_sessions_packageName_date
            ON app_usage_sessions (packageName, date)
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
    version = 4,
    exportSchema = false
)
abstract class MobileTrackDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun unlockDao(): UnlockDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun appOpenEventDao(): AppOpenEventDao
}

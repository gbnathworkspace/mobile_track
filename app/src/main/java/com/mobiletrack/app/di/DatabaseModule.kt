package com.mobiletrack.app.di

import android.content.Context
import androidx.room.Room
import com.mobiletrack.app.data.local.MIGRATION_1_2
import com.mobiletrack.app.data.local.MIGRATION_2_3
import com.mobiletrack.app.data.local.MobileTrackDatabase
import com.mobiletrack.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MobileTrackDatabase =
        Room.databaseBuilder(context, MobileTrackDatabase::class.java, "mobile_track.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAppUsageDao(db: MobileTrackDatabase): AppUsageDao = db.appUsageDao()
    @Provides fun provideUnlockDao(db: MobileTrackDatabase): UnlockDao = db.unlockDao()
    @Provides fun provideAppRuleDao(db: MobileTrackDatabase): AppRuleDao = db.appRuleDao()
    @Provides fun provideFocusSessionDao(db: MobileTrackDatabase): FocusSessionDao = db.focusSessionDao()
    @Provides fun provideAppOpenEventDao(db: MobileTrackDatabase): AppOpenEventDao = db.appOpenEventDao()
}

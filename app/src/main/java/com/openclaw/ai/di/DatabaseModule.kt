package com.openclaw.ai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.openclaw.ai.*
import com.openclaw.ai.data.db.AppDatabase
import com.openclaw.ai.data.db.dao.*
import com.openclaw.ai.proto.*
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "openclaw.db"
        ).build()
    }

    @Provides
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideSpaceDao(db: AppDatabase): SpaceDao = db.spaceDao()

    @Provides
    fun providePerChatSettingsDao(db: AppDatabase): PerChatSettingsDao = db.perChatSettingsDao()

    @Provides
    @Singleton
    fun provideSettingsSerializer(): Serializer<Settings> = SettingsSerializer

    @Provides
    @Singleton
    fun provideCutoutSerializer(): Serializer<CutoutCollection> = CutoutsSerializer

    @Provides
    @Singleton
    fun provideUserDataSerializer(): Serializer<UserData> = UserDataSerializer

    @Provides
    @Singleton
    fun provideBenchmarkResultsSerializer(): Serializer<BenchmarkResults> = BenchmarkResultsSerializer

    @Provides
    @Singleton
    fun provideSkillsSerializer(): Serializer<Skills> = SkillsSerializer

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        settingsSerializer: Serializer<Settings>,
    ): DataStore<Settings> {
        return DataStoreFactory.create(
            serializer = settingsSerializer,
            produceFile = { context.dataStoreFile("settings.pb") },
        )
    }

    @Provides
    @Singleton
    fun provideCutoutsDataStore(
        @ApplicationContext context: Context,
        cutoutsSerializer: Serializer<CutoutCollection>,
    ): DataStore<CutoutCollection> {
        return DataStoreFactory.create(
            serializer = cutoutsSerializer,
            produceFile = { context.dataStoreFile("cutouts.pb") },
        )
    }

    @Provides
    @Singleton
    fun provideUserDataDataStore(
        @ApplicationContext context: Context,
        userDataSerializer: Serializer<UserData>,
    ): DataStore<UserData> {
        return DataStoreFactory.create(
            serializer = userDataSerializer,
            produceFile = { context.dataStoreFile("user_data.pb") },
        )
    }

    @Provides
    @Singleton
    fun provideBenchmarkResultsDataStore(
        @ApplicationContext context: Context,
        benchmarkResultsSerializer: Serializer<BenchmarkResults>,
    ): DataStore<BenchmarkResults> {
        return DataStoreFactory.create(
            serializer = benchmarkResultsSerializer,
            produceFile = { context.dataStoreFile("benchmark_results.pb") },
        )
    }

    @Provides
    @Singleton
    fun provideSkillsDataStore(
        @ApplicationContext context: Context,
        skillsSerializer: Serializer<Skills>,
    ): DataStore<Skills> {
        return DataStoreFactory.create(
            serializer = skillsSerializer,
            produceFile = { context.dataStoreFile("skills.pb") },
        )
    }
}

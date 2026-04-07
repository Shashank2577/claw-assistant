package com.openclaw.ai.di

import com.openclaw.ai.AppLifecycleProvider
import com.openclaw.ai.GalleryLifecycleProvider
import com.openclaw.ai.data.DataStoreRepository
import com.openclaw.ai.data.DefaultDataStoreRepository
import com.openclaw.ai.data.DefaultDownloadRepository
import com.openclaw.ai.data.DownloadRepository
import com.openclaw.ai.data.repository.ConversationRepository
import com.openclaw.ai.data.repository.ModelRepository
import com.openclaw.ai.data.repository.SettingsRepository
import com.openclaw.ai.data.repository.SpaceRepository
import com.openclaw.ai.data.repository.impl.ConversationRepositoryImpl
import com.openclaw.ai.data.repository.impl.ModelRepositoryImpl
import com.openclaw.ai.data.repository.impl.SettingsRepositoryImpl
import com.openclaw.ai.data.repository.impl.SpaceRepositoryImpl
import com.openclaw.ai.runtime.LiteRtModelHelper
import com.openclaw.ai.runtime.LlmModelHelper
import com.openclaw.ai.tools.DefaultToolRegistry
import com.openclaw.ai.tools.ToolRegistry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindSpaceRepository(impl: SpaceRepositoryImpl): SpaceRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindToolRegistry(impl: DefaultToolRegistry): ToolRegistry

    @Binds
    @Singleton
    abstract fun bindLlmModelHelper(impl: LiteRtModelHelper): LlmModelHelper

    @Binds
    @Singleton
    abstract fun bindAppLifecycleProvider(impl: GalleryLifecycleProvider): AppLifecycleProvider

    @Binds
    @Singleton
    abstract fun bindDataStoreRepository(impl: DefaultDataStoreRepository): DataStoreRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DefaultDownloadRepository): DownloadRepository
}

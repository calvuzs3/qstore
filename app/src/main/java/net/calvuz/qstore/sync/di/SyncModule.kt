package net.calvuz.qstore.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.sync.data.SyncLocalStore
import net.calvuz.qstore.sync.data.repository.SyncRepositoryImpl
import net.calvuz.qstore.sync.domain.repository.SyncRepository
import net.calvuz.qstore.sync.domain.repository.SyncSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindSyncSettingsRepository(impl: SyncLocalStore): SyncSettingsRepository
}

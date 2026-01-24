package net.calvuz.qstore.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.app.data.repository.RecognitionSettingsRepositoryImpl
import net.calvuz.qstore.app.domain.repository.RecognitionSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecognitionSettingsRepository(
        recognitionSettingsRepositoryImpl: RecognitionSettingsRepositoryImpl
    ): RecognitionSettingsRepository
}

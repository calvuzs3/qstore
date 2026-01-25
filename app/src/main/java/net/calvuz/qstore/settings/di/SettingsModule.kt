package net.calvuz.qstore.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.settings.data.RecognitionSettingsRepositoryImpl
import net.calvuz.qstore.settings.data.DisplaySettingsRepositoryImpl
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository
import net.calvuz.qstore.settings.domain.repository.RecognitionSettingsRepository
import javax.inject.Singleton

/**
 * Modulo Hilt per le dipendenze del feature Settings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    /**
     * Binding per DisplaySettingsRepository.
     * L'implementazione è Singleton per condividere lo stato tra le schermate.
     */
    @Binds
    @Singleton
    abstract fun bindDisplaySettingsRepository(
        impl: DisplaySettingsRepositoryImpl
    ): DisplaySettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecognitionSettingsRepository(
        impl: RecognitionSettingsRepositoryImpl
    ): RecognitionSettingsRepository
}

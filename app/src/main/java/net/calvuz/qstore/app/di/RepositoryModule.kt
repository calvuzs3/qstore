package net.calvuz.qstore.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.app.data.mapper.ArticleImageMapper
import net.calvuz.qstore.app.data.mapper.ArticleMapper
import net.calvuz.qstore.app.data.mapper.LocationMapper
import net.calvuz.qstore.app.data.mapper.MovementMapper
import net.calvuz.qstore.categories.data.repository.ArticleCategoryRepositoryImpl
import net.calvuz.qstore.app.data.repository.ArticleRepositoryImpl
import net.calvuz.qstore.app.data.repository.ImageRecognitionRepositoryImpl
import net.calvuz.qstore.app.data.repository.InventoryRepositoryImpl
import net.calvuz.qstore.app.data.repository.LocationRepositoryImpl
import net.calvuz.qstore.app.data.repository.MovementRepositoryImpl
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.repository.ImageRecognitionRepository
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import net.calvuz.qstore.app.domain.repository.LocationRepository
import net.calvuz.qstore.app.domain.repository.MovementRepository
import net.calvuz.qstore.settings.domain.repository.RecognitionSettingsRepository
import net.calvuz.qstore.settings.domain.usecase.ApplyRecognitionPresetUseCase
import net.calvuz.qstore.settings.domain.usecase.GetRecognitionSettingsUseCase
import net.calvuz.qstore.settings.domain.usecase.ResetRecognitionSettingsUseCase
import net.calvuz.qstore.settings.domain.usecase.UpdateRecognitionSettingsUseCase
import javax.inject.Singleton

/**
 * Modulo Hilt per Repository e Mapper
 *
 * Fornisce:
 * - Repository implementations
 * - Mappers Entity ↔ Domain
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindArticleRepository(
        impl: ArticleRepositoryImpl
    ): ArticleRepository

    @Binds
    abstract fun bindArticleCategoryRepository(
        impl: ArticleCategoryRepositoryImpl
    ): ArticleCategoryRepository

    @Binds
    abstract fun bindMovementRepository(
        impl: MovementRepositoryImpl
    ): MovementRepository

    @Binds
    abstract fun bindImageRecognitionRepository(
        impl: ImageRecognitionRepositoryImpl
    ): ImageRecognitionRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        impl: InventoryRepositoryImpl
    ): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}


/**
 * Modulo per fornire Mappers
 * I mappers hanno @Inject constructor quindi vengono forniti automaticamente,
 * ma li dichiariamo esplicitamente per chiarezza
 */
@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    @Provides
    @Singleton
    fun provideArticleMapper(): ArticleMapper {
        return ArticleMapper()
    }

    @Provides
    @Singleton
    fun provideMovementMapper(): MovementMapper {
        return MovementMapper()
    }

    @Provides
    @Singleton
    fun provideArticleImageMapper(): ArticleImageMapper {
        return ArticleImageMapper()
    }

    @Provides
    @Singleton
    fun provideLocationMapper(): LocationMapper {
        return LocationMapper()
    }
}


@Module
@InstallIn(SingletonComponent::class)
object SettingsUseCaseModule {

    @Provides
    fun provideGetRecognitionSettingsUseCase(
        repository: RecognitionSettingsRepository
    ): GetRecognitionSettingsUseCase {
        return GetRecognitionSettingsUseCase(repository)
    }

    @Provides
    fun provideUpdateRecognitionSettingsUseCase(
        repository: RecognitionSettingsRepository
    ): UpdateRecognitionSettingsUseCase {
        return UpdateRecognitionSettingsUseCase(repository)
    }

    @Provides
    fun provideApplyRecognitionPresetUseCase(
        repository: RecognitionSettingsRepository
    ): ApplyRecognitionPresetUseCase {
        return ApplyRecognitionPresetUseCase(repository)
    }

    @Provides
    fun provideResetRecognitionSettingsUseCase(
        repository: RecognitionSettingsRepository
    ): ResetRecognitionSettingsUseCase {
        return ResetRecognitionSettingsUseCase(repository)
    }
}
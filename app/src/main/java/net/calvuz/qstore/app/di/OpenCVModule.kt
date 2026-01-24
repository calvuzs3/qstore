package net.calvuz.qstore.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.app.data.local.database.ArticleImageDao
import net.calvuz.qstore.app.data.opencv.ConfigurableImageMatcher
import net.calvuz.qstore.app.data.opencv.FeatureExtractor
import net.calvuz.qstore.app.data.opencv.ImageRecognitionValidator
import net.calvuz.qstore.app.data.opencv.OpenCVManager
import net.calvuz.qstore.app.domain.repository.RecognitionSettingsRepository
import javax.inject.Singleton

/**
 * Modulo Hilt per fornire dipendenze OpenCV
 */
@Module
@InstallIn(SingletonComponent::class)
object OpenCVModule {

    @Provides
    @Singleton
    fun provideFeatureExtractor(
        openCVManager: OpenCVManager
    ): FeatureExtractor {
        return FeatureExtractor(openCVManager)
    }

//    @Provides
//    @Singleton
//    fun provideImageMatcher(
//        openCVManager: OpenCVManager
//    ): ImageMatcher {
//        return ImageMatcher(openCVManager)
//    }

    // Rimuovi il provide per ImageMatcher, usa solo ConfigurableImageMatcher
    @Provides
    @Singleton
    fun provideConfigurableImageMatcher(
        openCVManager: OpenCVManager,
        settingsRepository: RecognitionSettingsRepository
    ): ConfigurableImageMatcher {
        return ConfigurableImageMatcher(openCVManager, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideImageRecognitionValidator(
        featureExtractor: FeatureExtractor,
        configurableImageMatcher: ConfigurableImageMatcher, // Cambiato parametro
        openCVManager: OpenCVManager,
        articleImageDao: ArticleImageDao
    ): ImageRecognitionValidator {
        return ImageRecognitionValidator(featureExtractor, configurableImageMatcher, openCVManager, articleImageDao)
    }

}
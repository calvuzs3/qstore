package net.calvuz.qstore.categories.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import net.calvuz.qstore.categories.domain.usecase.category.DeleteCategoryUseCase
import net.calvuz.qstore.categories.domain.usecase.category.GetCategoriesUseCase
import net.calvuz.qstore.categories.domain.usecase.category.SaveCategoryUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategoryUseCaseModule {

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(
        repository: ArticleCategoryRepository
    ): GetCategoriesUseCase {
        return GetCategoriesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSaveCategoryUseCase(
        repository: ArticleCategoryRepository
    ): SaveCategoryUseCase {
        return SaveCategoryUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteCategoryUseCase(
        repository: ArticleCategoryRepository
    ): DeleteCategoryUseCase {
        return DeleteCategoryUseCase(repository)
    }
}

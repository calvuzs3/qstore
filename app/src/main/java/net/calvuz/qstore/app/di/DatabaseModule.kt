package net.calvuz.qstore.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.categories.data.local.ArticleCategoryDao
import net.calvuz.qstore.app.data.local.database.ArticleDao
import net.calvuz.qstore.app.data.local.database.ArticleImageDao
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.data.local.database.MovementDao
import net.calvuz.qstore.app.data.local.database.QuickStoreDatabase
import net.calvuz.qstore.app.data.local.database.migration.MIGRATION_1_2
import net.calvuz.qstore.app.data.local.database.migration.MIGRATION_2_3
import javax.inject.Singleton

/**
 * Modulo Hilt per il database Room
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuickStoreDatabase(
        @ApplicationContext context: Context
    ): QuickStoreDatabase {
        return Room.databaseBuilder(
            context,
            QuickStoreDatabase::class.java,
            QuickStoreDatabase.DATABASE_NAME
        )
            //.fallbackToDestructiveMigration() // Solo per sviluppo, rimuovere in produzione
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideArticleDao(database: QuickStoreDatabase): ArticleDao {
        return database.articleDao()
    }

    @Provides
    @Singleton
    fun provideArticleCategoryDao(database: QuickStoreDatabase): ArticleCategoryDao {
        return database.articleCategoryDao()
    }

    @Provides
    @Singleton
    fun provideInventoryDao(database: QuickStoreDatabase): InventoryDao {
        return database.inventoryDao()
    }

    @Provides
    @Singleton
    fun provideMovementDao(database: QuickStoreDatabase): MovementDao {
        return database.movementDao()
    }

    @Provides
    @Singleton
    fun provideArticleImageDao(database: QuickStoreDatabase): ArticleImageDao {
        return database.articleImageDao()
    }
}
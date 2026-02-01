package net.calvuz.qstore.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import net.calvuz.qstore.app.data.local.entity.ArticleCategoryEntity
import net.calvuz.qstore.app.data.local.entity.ArticleEntity
import net.calvuz.qstore.app.data.local.entity.ArticleImageEntity
import net.calvuz.qstore.app.data.local.entity.InventoryEntity
import net.calvuz.qstore.app.data.local.entity.MovementEntity
import net.calvuz.qstore.categories.data.local.ArticleCategoryDao

/**
 * Database Room principale dell'applicazione
 */
@Database(
    entities = [
        ArticleEntity::class,
        ArticleCategoryEntity::class,
        InventoryEntity::class,
        MovementEntity::class,
        ArticleImageEntity::class
    ],
    version = QuickStoreDatabase.DATABASE_VERSION,
    exportSchema = false
)
//@TypeConverters(Converters::class)
abstract class QuickStoreDatabase: RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun articleCategoryDao(): ArticleCategoryDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun movementDao(): MovementDao
    abstract fun articleImageDao(): ArticleImageDao

    companion object {
        const val DATABASE_NAME = "warehouse_db"
        const val DATABASE_VERSION = 3
    }
}
package net.calvuz.qstore.app.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Migration per convertire article_images.id (Long auto-generated) a uuid (String)
 *
 * Questa migration:
 * 1. Crea una nuova tabella article_images_new con uuid come PK
 * 2. Copia i dati esistenti generando UUID per ogni riga
 * 3. Elimina la vecchia tabella
 * 4. Rinomina la nuova tabella
 * 5. Ricrea l'indice
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crea nuova tabella con UUID come PK
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS article_images_new (
                uuid TEXT PRIMARY KEY NOT NULL,
                article_uuid TEXT NOT NULL,
                image_path TEXT NOT NULL,
                features_data BLOB NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (article_uuid) REFERENCES articles(uuid) ON DELETE CASCADE
            )
        """.trimIndent()
        )

        // 2. Copia dati esistenti generando UUID per ogni riga
        // Nota: SQLite non ha una funzione UUID nativa, quindi usiamo una query
        // che legge i dati e li inserisce con UUID generato lato Kotlin
        val cursor =
            database.query("SELECT id, article_uuid, image_path, features_data, created_at FROM article_images")

        cursor.use {
            while (it.moveToNext()) {
                val articleUuid = it.getString(it.getColumnIndexOrThrow("article_uuid"))
                val imagePath = it.getString(it.getColumnIndexOrThrow("image_path"))
                val featuresData = it.getBlob(it.getColumnIndexOrThrow("features_data"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))

                // Genera nuovo UUID
                val newUuid = UUID.randomUUID().toString()

                // Inserisci nella nuova tabella
                database.execSQL(
                    """
                    INSERT INTO article_images_new (uuid, article_uuid, image_path, features_data, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(newUuid, articleUuid, imagePath, featuresData, createdAt)
                )
            }
        }

        // 3. Elimina vecchia tabella
        database.execSQL("DROP TABLE article_images")

        // 4. Rinomina nuova tabella
        database.execSQL("ALTER TABLE article_images_new RENAME TO article_images")

        // 5. Ricrea indice
        database.execSQL("CREATE INDEX IF NOT EXISTS index_article_images_article_uuid ON article_images(article_uuid)")
    }
}

/**
 * Migration da versione 1 a 2
 * - Aggiunge tabella article_categories
 * - Modifica articles: category -> category_id (FK)
 * - Aggiunge campi code_oem, code_erp, code_bm
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Crea tabella article_categories
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS article_categories (
                uuid TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """
        )

        // 2. Crea indice unique su name
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_article_categories_name 
            ON article_categories (name)
        """
        )

        // 3. Inserisci categorie predefinite
        // UUID generati una volta sola - NON cambiarli mai!
        val categoryAcqua = UUID.randomUUID().toString()
        val categoryAltro = UUID.randomUUID().toString()

        db.execSQL(
            """
            INSERT INTO article_categories (uuid, name, description, notes, created_at, updated_at)
            VALUES 
                ('$categoryAcqua', 'Acqua', 'Prodotti categoria Acqua', '', $now, $now),
                ('$categoryAltro', 'Altro', 'Categoria generica', '', $now, $now)
        """
        )

        // 4. Crea nuova tabella articles con struttura aggiornata
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS articles_new (
                uuid TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                category_id TEXT NOT NULL,
                unit_of_measure TEXT NOT NULL,
                reorder_level REAL NOT NULL DEFAULT 0.0,
                notes TEXT NOT NULL DEFAULT '',
                code_oem TEXT NOT NULL DEFAULT '',
                code_erp TEXT NOT NULL DEFAULT '',
                code_bm TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (category_id) REFERENCES article_categories(uuid) ON DELETE RESTRICT
            )
        """
        )

        // 5. Migra dati esistenti, mappando category string -> category_id
        db.execSQL(
            """
            INSERT INTO articles_new (
                uuid, name, description, category_id, unit_of_measure, 
                reorder_level, notes, code_oem, code_erp, code_bm, 
                created_at, updated_at
            )
            SELECT 
                uuid, name, description,
                CASE 
                    WHEN category = 'Acqua' THEN '$categoryAcqua'
                    ELSE '$categoryAltro'
                END as category_id,
                unit_of_measure, reorder_level, notes,
                '' as code_oem, '' as code_erp, '' as code_bm,
                created_at, updated_at
            FROM articles
        """
        )

        // 6. Elimina vecchia tabella e rinomina
        db.execSQL("DROP TABLE articles")
        db.execSQL("ALTER TABLE articles_new RENAME TO articles")

        // 7. Ricrea indici su articles
        db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_category_id ON articles (category_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_name ON articles (name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_code_oem ON articles (code_oem)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_code_erp ON articles (code_erp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_code_bm ON articles (code_bm)")
    }
}


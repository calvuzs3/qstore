package net.calvuz.qstore.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Migration da versione X a X+1
 * - Aggiunge tabella article_categories
 * - Modifica articles: category -> category_id (FK)
 * - Aggiunge campi code_oem, code_erp, code_bm
 */
val MIGRATION_1_2 = object : Migration(1, 2) {  // Sostituisci X e Y con i numeri corretti!
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Crea tabella article_categories
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS article_categories (
                uuid TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)

        // 2. Crea indice unique su name
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_article_categories_name 
            ON article_categories (name)
        """)

        // 3. Inserisci categorie predefinite
        // UUID generati una volta sola - NON cambiarli mai!
        val categoryAcqua = UUID.randomUUID().toString()
        val categoryAltro = UUID.randomUUID().toString()

        db.execSQL("""
            INSERT INTO article_categories (uuid, name, description, notes, created_at, updated_at)
            VALUES 
                ('$categoryAcqua', 'Acqua', 'Prodotti categoria Acqua', '', $now, $now),
                ('$categoryAltro', 'Altro', 'Categoria generica', '', $now, $now)
        """)

        // 4. Crea nuova tabella articles con struttura aggiornata
        db.execSQL("""
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
        """)

        // 5. Migra dati esistenti, mappando category string -> category_id
        db.execSQL("""
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
        """)

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
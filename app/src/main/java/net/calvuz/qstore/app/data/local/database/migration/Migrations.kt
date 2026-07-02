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

/**
 * Migration da versione 3 a 4 - Multi-magazzino (ubicazioni)
 *
 * - Aggiunge tabella locations (magazzini/ubicazioni) con una ubicazione di default
 *   ("Magazzino principale") a cui vengono assegnati tutti i movimenti/giacenze esistenti
 * - Aggiunge tabella article_location_thresholds (soglia di riordino opzionale per
 *   coppia articolo/ubicazione)
 * - Rebuild di movements: id da Long autoGenerate a TEXT (UUID), aggiunge
 *   from_location_uuid/to_location_uuid. I vecchi movimenti IN prendono
 *   to_location_uuid = ubicazione di default, i vecchi OUT prendono
 *   from_location_uuid = ubicazione di default — coerente con la regola per tipo
 *   (necessario perché in futuro il sync validerà questa combinazione)
 * - Rebuild di inventory: da PK singola (article_uuid) a chiave composta
 *   (article_uuid, location_uuid), tutte le righe esistenti assegnate all'ubicazione
 *   di default
 *
 * NON aggiunge org_id/created_by: non esiste ancora un concetto di org/utente loggato
 * lato client (arriveranno in una migrazione successiva insieme al modulo auth).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Crea tabella locations
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS locations (
                uuid TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_locations_name ON locations (name)")

        // 2. Ubicazione di default per i dati esistenti
        val defaultLocationUuid = UUID.randomUUID().toString()
        database.execSQL(
            """
            INSERT INTO locations (uuid, name, notes, created_at, updated_at)
            VALUES ('$defaultLocationUuid', 'Magazzino principale', '', $now, $now)
            """.trimIndent()
        )

        // 3. Crea tabella article_location_thresholds (vuota, feature opt-in)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS article_location_thresholds (
                uuid TEXT NOT NULL PRIMARY KEY,
                article_uuid TEXT NOT NULL,
                location_uuid TEXT NOT NULL,
                reorder_level REAL NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (article_uuid) REFERENCES articles(uuid) ON DELETE CASCADE,
                FOREIGN KEY (location_uuid) REFERENCES locations(uuid) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_article_location_thresholds_article_uuid_location_uuid " +
                "ON article_location_thresholds (article_uuid, location_uuid)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_article_location_thresholds_location_uuid " +
                "ON article_location_thresholds (location_uuid)"
        )

        // 4. Rebuild movements: id Long -> TEXT (UUID), + from/to location
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS movements_new (
                id TEXT NOT NULL PRIMARY KEY,
                article_uuid TEXT NOT NULL,
                type TEXT NOT NULL,
                from_location_uuid TEXT,
                to_location_uuid TEXT,
                quantity REAL NOT NULL,
                notes TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (article_uuid) REFERENCES articles(uuid) ON DELETE CASCADE,
                FOREIGN KEY (from_location_uuid) REFERENCES locations(uuid) ON DELETE RESTRICT,
                FOREIGN KEY (to_location_uuid) REFERENCES locations(uuid) ON DELETE RESTRICT
            )
            """.trimIndent()
        )

        val movementsCursor = database.query("SELECT id, article_uuid, type, quantity, notes, created_at FROM movements")
        movementsCursor.use {
            while (it.moveToNext()) {
                val articleUuid = it.getString(it.getColumnIndexOrThrow("article_uuid"))
                val type = it.getString(it.getColumnIndexOrThrow("type"))
                val quantity = it.getDouble(it.getColumnIndexOrThrow("quantity"))
                val notes = it.getString(it.getColumnIndexOrThrow("notes"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))

                val newId = UUID.randomUUID().toString()
                val fromLocationUuid = if (type == "OUT") defaultLocationUuid else null
                val toLocationUuid = if (type == "IN") defaultLocationUuid else null

                database.execSQL(
                    """
                    INSERT INTO movements_new (id, article_uuid, type, from_location_uuid, to_location_uuid, quantity, notes, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(newId, articleUuid, type, fromLocationUuid, toLocationUuid, quantity, notes, createdAt)
                )
            }
        }

        database.execSQL("DROP TABLE movements")
        database.execSQL("ALTER TABLE movements_new RENAME TO movements")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_movements_article_uuid ON movements (article_uuid)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_movements_created_at ON movements (created_at)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_movements_from_location_uuid ON movements (from_location_uuid)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_movements_to_location_uuid ON movements (to_location_uuid)")

        // 5. Rebuild inventory: PK singola (article_uuid) -> composta (article_uuid, location_uuid)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS inventory_new (
                article_uuid TEXT NOT NULL,
                location_uuid TEXT NOT NULL,
                current_quantity REAL NOT NULL,
                last_movement_at INTEGER NOT NULL,
                PRIMARY KEY (article_uuid, location_uuid),
                FOREIGN KEY (article_uuid) REFERENCES articles(uuid) ON DELETE CASCADE,
                FOREIGN KEY (location_uuid) REFERENCES locations(uuid) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO inventory_new (article_uuid, location_uuid, current_quantity, last_movement_at)
            SELECT article_uuid, '$defaultLocationUuid', current_quantity, last_movement_at
            FROM inventory
            """.trimIndent()
        )

        database.execSQL("DROP TABLE inventory")
        database.execSQL("ALTER TABLE inventory_new RENAME TO inventory")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_location_uuid ON inventory (location_uuid)")
    }
}

/**
 * Migration da versione 4 a 5 - Attribuzione utente sui movimenti
 *
 * Aggiunge movements.created_by (nullable): colonna additiva semplice, nessun rebuild di
 * tabella necessario (a differenza delle migrazioni precedenti che cambiavano tipo/PK).
 * Le righe esistenti restano con created_by = NULL — il sync client, al momento del
 * push, le attribuisce all'utente della sessione corrente come fallback.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE movements ADD COLUMN created_by TEXT DEFAULT NULL")
    }
}


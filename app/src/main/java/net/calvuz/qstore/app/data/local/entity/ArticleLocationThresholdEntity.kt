package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity per la tabella article_location_thresholds - Soglia di riordino opzionale per
 * una coppia (articolo, ubicazione), es. "scorta minima sul furgone". Riga opzionale: se
 * assente per una coppia, vale solo articles.reorderLevel confrontato con la giacenza
 * totale su tutte le ubicazioni.
 */
@Entity(
    tableName = "article_location_thresholds",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["article_uuid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["location_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["article_uuid", "location_uuid"], unique = true),
        // location_uuid da solo non è coperto dall'indice composto sopra (non è il prefisso
        // sinistro) — serve un indice dedicato per evitare full table scan sulla FK.
        Index(value = ["location_uuid"])
    ]
)
data class ArticleLocationThresholdEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "location_uuid")
    val locationUuid: String,

    @ColumnInfo(name = "reorder_level")
    val reorderLevel: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // Nessun flusso di cancellazione locale usa ancora questo campo (nessuna UI oggi) —
    // aggiunto per simmetria con lo schema server, pronto per quando servirà.
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
)

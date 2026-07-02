package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qstore.app.domain.model.enum.MovementType

/**
 * Entity per la tabella movements - Storico movimentazioni magazzino
 *
 * APPEND-ONLY: nessun update, nessun delete previsto nel flusso normale — la giacenza
 * per ubicazione si ricalcola sempre rigiocando questi movimenti, mai modificandoli.
 *
 * Regola per tipo (from/to location):
 *   IN:         solo toLocationUuid
 *   OUT:        solo fromLocationUuid
 *   ADJUSTMENT: uno solo dei due (aumento->to, diminuzione->from)
 *   TRANSFER:   entrambi, diversi tra loro
 */
@Entity(
    tableName = "movements",
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
            childColumns = ["from_location_uuid"],
            onDelete = ForeignKey.RESTRICT // log storico: non si cancella una ubicazione con movimenti
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["to_location_uuid"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["article_uuid"]),
        Index(value = ["created_at"]),
        Index(value = ["from_location_uuid"]),
        Index(value = ["to_location_uuid"])
    ]
)
data class MovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID — era Long autoGenerate, cambiato per compatibilità futura col sync multi-device

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "type")
    val type: MovementType,

    @ColumnInfo(name = "from_location_uuid")
    val fromLocationUuid: String?,

    @ColumnInfo(name = "to_location_uuid")
    val toLocationUuid: String?,

    @ColumnInfo(name = "quantity")
    val quantity: Double, // Double per supportare decimali

    @ColumnInfo(name = "notes")
    val notes: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp UTC in milliseconds

    // Nullable: l'app resta utilizzabile offline senza account, i movimenti creati senza
    // sessione attiva non hanno un utente da attribuire finché non si fa login. Il sync
    // client, al momento del push, usa l'utente della sessione corrente come fallback per
    // le righe storiche con questo campo nullo.
    @ColumnInfo(name = "created_by")
    val createdBy: String? = null
)

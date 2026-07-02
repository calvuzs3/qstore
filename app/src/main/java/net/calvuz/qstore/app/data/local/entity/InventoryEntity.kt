package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity per la tabella inventory - Giacenze articoli per ubicazione
 *
 * Separata da articles per evitare di aggiornare il timestamp dell'articolo
 * ad ogni movimentazione di magazzino.
 * Chiave composta (article_uuid, location_uuid): un articolo può avere una giacenza
 * diversa per ogni magazzino/ubicazione (es. sede vs furgone). La giacenza totale di un
 * articolo si ottiene sommando le righe su tutte le ubicazioni.
 */
@Entity(
    tableName = "inventory",
    primaryKeys = ["article_uuid", "location_uuid"],
    // article_uuid è già coperto come prefisso della PK composta; location_uuid da solo
    // no, serve un indice dedicato per evitare full table scan sulla FK.
    indices = [Index(value = ["location_uuid"])],
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
    ]
)
data class InventoryEntity(
    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "location_uuid")
    val locationUuid: String,

    @ColumnInfo(name = "current_quantity")
    val currentQuantity: Double, // Double per supportare decimali (es: 2.5 kg)

    @ColumnInfo(name = "last_movement_at")
    val lastMovementAt: Long // Unix timestamp UTC dell'ultima movimentazione
)

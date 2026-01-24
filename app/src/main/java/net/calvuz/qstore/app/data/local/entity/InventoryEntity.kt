package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entity per la tabella inventory - Giacenze articoli
 *
 * Separata da articles per evitare di aggiornare il timestamp dell'articolo
 * ad ogni movimentazione di magazzino.
 * Relazione 1:1 con ArticleEntity.
 */
@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["article_uuid"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
data class InventoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "current_quantity")
    val currentQuantity: Double, // Double per supportare decimali (es: 2.5 kg)

    @ColumnInfo(name = "last_movement_at")
    val lastMovementAt: Long // Unix timestamp UTC dell'ultima movimentazione
)
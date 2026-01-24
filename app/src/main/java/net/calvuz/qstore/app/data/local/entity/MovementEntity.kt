package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.calvuz.qstore.app.domain.model.enum.MovementType

/**
 * Entity per la tabella movements - Storico movimentazioni magazzino
 */
@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["article_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["article_uuid"]),
        Index(value = ["created_at"])
    ]
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "type")
    val type: MovementType,

    @ColumnInfo(name = "quantity")
    val quantity: Double, // Double per supportare decimali

    @ColumnInfo(name = "notes")
    val notes: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long // Unix timestamp UTC in milliseconds
)
package net.calvuz.qstore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = ArticleCategoryEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT  // Impedisce cancellazione categoria se ha articoli
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["name"]),
        Index(value = ["code_oem"]),
        Index(value = ["code_erp"]),
        Index(value = ["code_bm"])
    ]
)
data class ArticleEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "category_id")
    val categoryId: String,  // FK -> article_categories.uuid

    @ColumnInfo(name = "unit_of_measure")
    val unitOfMeasure: String,

    @ColumnInfo(name = "reorder_level")
    val reorderLevel: Double = 0.0,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    // Nuovi campi per codici esterni
    @ColumnInfo(name = "code_oem")
    val codeOEM: String = "",

    @ColumnInfo(name = "code_erp")
    val codeERP: String = "",

    @ColumnInfo(name = "code_bm")
    val codeBM: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
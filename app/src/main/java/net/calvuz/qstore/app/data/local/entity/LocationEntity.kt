package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity per la tabella locations - Magazzini/ubicazioni (es. "Sede", "Furgone Mario")
 */
@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class LocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // Nessun flusso di cancellazione locale usa ancora questo campo (nessuna UI oggi) —
    // aggiunto per simmetria con lo schema server, pronto per quando servirà.
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
)

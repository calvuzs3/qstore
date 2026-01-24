package net.calvuz.qstore.app.data.local.database

/**
 * Type Converters per Room Database
 *
 * Room supporta solo tipi primitivi, quindi convertiamo:
 * - Enum → String (tramite .name e valueOf())
 * - Altri tipi custom se necessari
 *
 * Note: MovementType non serve più converter perché
 * salviamo direttamente String nel DB e convertiamo
 * solo a livello Mapper (Entity ↔ Domain)
 */
class Converters {

    // Aggiungi qui altri converters se necessari in futuro
    // Esempio:
    // @TypeConverter
    // fun fromTimestamp(value: Long?): Date? {
    //     return value?.let { Date(it) }
    // }

}
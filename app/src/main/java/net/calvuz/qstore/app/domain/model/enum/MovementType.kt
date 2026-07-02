package net.calvuz.qstore.app.domain.model.enum

/**
 * Enum per tipo di movimento
 */
enum class MovementType {
    IN,         // Carico - aumenta giacenza (solo toLocationUuid)
    OUT,        // Scarico - diminuisce giacenza (solo fromLocationUuid)
    ADJUSTMENT, // Rettifica manuale - uno solo tra from/to (aumento->to, diminuzione->from)
    TRANSFER    // Spostamento atomico tra due ubicazioni proprie (entrambi from e to)
}
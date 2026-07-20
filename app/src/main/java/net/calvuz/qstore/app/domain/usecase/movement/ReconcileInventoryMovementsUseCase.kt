package net.calvuz.qstore.app.domain.usecase.movement

import kotlinx.coroutines.flow.first
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import net.calvuz.qstore.app.domain.repository.MovementRepository
import net.calvuz.qstore.auth.domain.usecase.ObserveSessionUseCase
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/**
 * Tool di manutenzione (solo debug, vedi LoginScreen): confronta ogni riga di inventory con
 * il netto dei movimenti registrati per la stessa coppia (articolo, ubicazione) e, dove non
 * coincidono, crea un movimento ADJUSTMENT correttivo — senza toccare inventory, che ha già
 * il valore giusto (l'unico problema è che manca lo storico che lo giustifica e che il sync
 * potrebbe propagare). Nato per riparare gli articoli creati prima del fix di
 * AddArticleUseCase, quando la giacenza iniziale veniva scritta direttamente in inventory
 * senza generare alcun movimento — vedi memoria bug-initial-quantity-no-movement.
 *
 * createdAt è sempre "adesso": un timestamp storico (es. la data di creazione articolo)
 * finirebbe prima del cursore sincePush già avanzato da sync precedenti e non verrebbe mai
 * pushato — stessa classe di bug del clock-skew già risolto altrove nel sync.
 */
class ReconcileInventoryMovementsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val movementRepository: MovementRepository,
    private val observeSessionUseCase: ObserveSessionUseCase
) {
    suspend operator fun invoke(): Result<Int> {
        return try {
            val entries = inventoryRepository.getAllEntries()
            val movements = movementRepository.getAllMovements().getOrElse { return Result.failure(it) }

            val netByLocation = mutableMapOf<Pair<String, String>, Double>()
            movements.forEach { movement ->
                movement.toLocationUuid?.let { location ->
                    val key = movement.articleUuid to location
                    netByLocation[key] = (netByLocation[key] ?: 0.0) + movement.quantity
                }
                movement.fromLocationUuid?.let { location ->
                    val key = movement.articleUuid to location
                    netByLocation[key] = (netByLocation[key] ?: 0.0) - movement.quantity
                }
            }

            val createdBy = observeSessionUseCase().first()?.userId
            var fixedCount = 0

            entries.forEach { entry ->
                val key = entry.articleUuid to entry.locationUuid
                val expected = netByLocation[key] ?: 0.0
                val delta = entry.currentQuantity - expected
                if (abs(delta) < 1e-9) return@forEach

                val correction = Movement(
                    id = UUID.randomUUID().toString(),
                    articleUuid = entry.articleUuid,
                    type = MovementType.ADJUSTMENT,
                    fromLocationUuid = if (delta < 0) entry.locationUuid else null,
                    toLocationUuid = if (delta > 0) entry.locationUuid else null,
                    quantity = abs(delta),
                    notes = "Correzione: giacenza non coperta da movimenti (backfill)",
                    createdAt = System.currentTimeMillis(),
                    createdBy = createdBy
                )

                movementRepository.insertMovementRecord(correction).getOrElse { return Result.failure(it) }
                fixedCount++
            }

            Result.success(fixedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

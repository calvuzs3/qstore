package net.calvuz.qstore.app.domain.usecase.movement

import kotlinx.coroutines.flow.first
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.repository.LocationRepository
import net.calvuz.qstore.app.domain.repository.MovementRepository
import net.calvuz.qstore.auth.domain.usecase.ObserveSessionUseCase
import javax.inject.Inject

/**
 * Use Case per registrare una movimentazione di magazzino
 *
 * Questa operazione è transazionale:
 * - Inserisce il movimento nello storico
 * - Aggiorna l'inventario dell'articolo per l'ubicazione interessata
 */
class AddMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val articleRepository: ArticleRepository,
    private val locationRepository: LocationRepository,
    private val observeSessionUseCase: ObserveSessionUseCase
) {
    /**
     * Registra una movimentazione IN/OUT sull'ubicazione di default (la prima disponibile).
     * Non esiste ancora una UI per scegliere l'ubicazione — questo overload resta identico
     * a prima della migrazione multi-magazzino per non rompere i chiamanti esistenti.
     *
     * @param articleUuid UUID dell'articolo
     * @param type Tipo movimentazione (IN/OUT)
     * @param quantity Quantità (sempre positiva)
     * @param notes Note descrittive
     * @return Result con Movement creato, errore altrimenti
     */
    suspend operator fun invoke(
        articleUuid: String,
        type: MovementType,
        quantity: Double,
        notes: String = ""
    ): Result<Movement> {
        require(type == MovementType.IN || type == MovementType.OUT) {
            "Questo overload supporta solo IN/OUT; usa invoke(articleUuid, type, fromLocationUuid, toLocationUuid, quantity, notes) per ADJUSTMENT/TRANSFER"
        }

        val defaultLocationUuid = locationRepository.getAll()
            .getOrElse { return Result.failure(it) }
            .firstOrNull()?.uuid
            ?: return Result.failure(IllegalStateException("Nessuna ubicazione disponibile"))

        return invoke(
            articleUuid = articleUuid,
            type = type,
            fromLocationUuid = if (type == MovementType.OUT) defaultLocationUuid else null,
            toLocationUuid = if (type == MovementType.IN) defaultLocationUuid else null,
            quantity = quantity,
            notes = notes
        )
    }

    /**
     * Registra una movimentazione con ubicazioni esplicite — copre anche ADJUSTMENT e TRANSFER.
     *
     * @param fromLocationUuid richiesto per OUT/TRANSFER, opzionale per ADJUSTMENT in diminuzione
     * @param toLocationUuid richiesto per IN/TRANSFER, opzionale per ADJUSTMENT in aumento
     */
    suspend operator fun invoke(
        articleUuid: String,
        type: MovementType,
        fromLocationUuid: String?,
        toLocationUuid: String?,
        quantity: Double,
        notes: String = ""
    ): Result<Movement> {
        // Validazione input
        if (articleUuid.isBlank()) {
            return Result.failure(IllegalArgumentException("Article UUID cannot be blank"))
        }

        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be positive"))
        }

        // Verifica che l'articolo esista
        val articleExists = articleRepository.getByUuid(articleUuid)
            .getOrElse {
                return Result.failure(it)
            }

        if (articleExists == null) {
            return Result.failure(IllegalArgumentException("Article not found"))
        }

        // null se non loggato: l'app resta utilizzabile offline senza account, il sync
        // client attribuirà queste righe storiche all'utente della sessione al momento del push.
        val createdBy = observeSessionUseCase().first()?.userId

        // Registra movimento (transazionale con update inventario per ubicazione,
        // include il controllo di disponibilità per i debiti — vedi MovementRepositoryImpl)
        val result = movementRepository.addMovement(
            articleUuid = articleUuid,
            type = type,
            fromLocationUuid = fromLocationUuid,
            toLocationUuid = toLocationUuid,
            quantity = quantity,
            notes = notes.trim(),
            createdBy = createdBy
        )

        return result.map {
            Movement(
                id = "", // il repository genera l'id reale; questo Result serve solo da conferma
                articleUuid = articleUuid,
                type = type,
                fromLocationUuid = fromLocationUuid,
                toLocationUuid = toLocationUuid,
                quantity = quantity,
                notes = notes.trim(),
                createdAt = System.currentTimeMillis(),
                createdBy = createdBy
            )
        }
    }

    /**
     * Shortcut per registrare un'entrata sull'ubicazione di default
     */
    suspend fun addIncoming(
        articleUuid: String,
        quantity: Double,
        note: String = ""
    ): Result<Movement> {
        return invoke(articleUuid, MovementType.IN, quantity, note)
    }

    /**
     * Shortcut per registrare un'uscita dall'ubicazione di default
     */
    suspend fun addOutgoing(
        articleUuid: String,
        quantity: Double,
        note: String = ""
    ): Result<Movement> {
        return invoke(articleUuid, MovementType.OUT, quantity, note)
    }
}

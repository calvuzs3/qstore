package net.calvuz.qstore.app.domain.usecase.movement

import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.repository.MovementRepository
import javax.inject.Inject

/**
 * Use Case per registrare una movimentazione di magazzino
 *
 * Questa operazione è transazionale:
 * - Inserisce il movimento nello storico
 * - Aggiorna l'inventario dell'articolo
 */
class AddMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val articleRepository: ArticleRepository
) {
    /**
     * Registra una movimentazione (entrata o uscita)
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

        // Verifica disponibilità per uscite
        if (type == MovementType.OUT) {
            val inventory = articleRepository.getInventory(articleUuid)
                .getOrElse {
                    return Result.failure(it)
                }

            if (inventory == null) {
                return Result.failure(IllegalStateException("Inventory not found"))
            }

            if (inventory.currentQuantity < quantity) {
                return Result.failure(
                    IllegalArgumentException(
                        "Insufficient quantity. Available: ${inventory.currentQuantity}, Requested: $quantity"
                    )
                )
            }
        }

        // Crea movimento
        val movement = Movement(
            id = 0,
            articleUuid = articleUuid,
            type = type,
            quantity = quantity,
            notes = notes.trim(),
            createdAt  = System.currentTimeMillis()
        )

        // Registra movimento (transazionale con update inventory)
        return movementRepository.addMovement(movement)
            .map { movement }
    }

    /**
     * Shortcut per registrare un'entrata
     */
    suspend fun addIncoming(
        articleUuid: String,
        quantity: Double,
        note: String = ""
    ): Result<Movement> {
        return invoke(articleUuid, MovementType.IN, quantity, note)
    }

    /**
     * Shortcut per registrare un'uscita
     */
    suspend fun addOutgoing(
        articleUuid: String,
        quantity: Double,
        note: String = ""
    ): Result<Movement> {
        return invoke(articleUuid, MovementType.OUT, quantity, note)
    }
}
package net.calvuz.qstore.app.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.data.local.database.MovementDao
import net.calvuz.qstore.app.data.local.database.QuickStoreDatabase
import net.calvuz.qstore.app.data.local.entity.InventoryEntity
import net.calvuz.qstore.app.data.local.entity.MovementEntity
import net.calvuz.qstore.app.data.mapper.MovementMapper
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.repository.MovementRepository
import java.util.UUID
import javax.inject.Inject


/**
 * Implementazione del repository per movimentazioni
 *
 * L'aggiornamento inventario è un algoritmo unico basato su quali campi location sono
 * valorizzati, non sul `type` — copre IN/OUT/ADJUSTMENT/TRANSFER senza branching per tipo:
 *   fromLocationUuid != null -> debita quantity da (article, fromLocation), errore se
 *                                andrebbe sotto zero
 *   toLocationUuid   != null -> accredita quantity a (article, toLocation)
 * Un TRANSFER valorizza entrambi (debito+credito nella stessa transazione, atomico).
 */
class MovementRepositoryImpl @Inject constructor(
    private val database: QuickStoreDatabase,
    private val movementDao: MovementDao,
    private val inventoryDao: InventoryDao,
    private val movementMapper: MovementMapper
) : MovementRepository {

    override suspend fun addMovement(movement: Movement): Result<Unit> {
        return try {
            database.withTransaction {
                validateLocationsForType(movement.type, movement.fromLocationUuid, movement.toLocationUuid)
                applyInventoryDelta(movement.articleUuid, movement.fromLocationUuid, movement.toLocationUuid, movement.quantity, movement.createdAt)
                movementDao.insert(movementMapper.toEntity(movement))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMovement(
        articleUuid: String,
        type: MovementType,
        fromLocationUuid: String?,
        toLocationUuid: String?,
        quantity: Double,
        notes: String
    ): Result<Unit> {
        return try {
            database.withTransaction {
                validateLocationsForType(type, fromLocationUuid, toLocationUuid)
                val now = System.currentTimeMillis()
                applyInventoryDelta(articleUuid, fromLocationUuid, toLocationUuid, quantity, now)
                movementDao.insert(
                    MovementEntity(
                        id = UUID.randomUUID().toString(),
                        articleUuid = articleUuid,
                        type = type,
                        fromLocationUuid = fromLocationUuid,
                        toLocationUuid = toLocationUuid,
                        quantity = quantity,
                        notes = notes,
                        createdAt = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateLocationsForType(type: MovementType, from: String?, to: String?) {
        val valid = when (type) {
            MovementType.IN -> from == null && to != null
            MovementType.OUT -> from != null && to == null
            MovementType.ADJUSTMENT -> (from == null) != (to == null)
            MovementType.TRANSFER -> from != null && to != null && from != to
        }
        require(valid) { "Combinazione type/fromLocationUuid/toLocationUuid non valida per $type (from=$from, to=$to)" }
    }

    private suspend fun applyInventoryDelta(
        articleUuid: String,
        fromLocationUuid: String?,
        toLocationUuid: String?,
        quantity: Double,
        timestamp: Long
    ) {
        if (fromLocationUuid != null) {
            val inventory = inventoryDao.getByArticleAndLocation(articleUuid, fromLocationUuid)
                ?: InventoryEntity(articleUuid, fromLocationUuid, 0.0, timestamp)
            val newQuantity = inventory.currentQuantity - quantity
            if (newQuantity < 0) {
                throw IllegalArgumentException(
                    "Insufficient quantity at location $fromLocationUuid. Current: ${inventory.currentQuantity}, Requested: $quantity"
                )
            }
            upsertInventory(inventory.copy(currentQuantity = newQuantity, lastMovementAt = timestamp))
        }
        if (toLocationUuid != null) {
            val inventory = inventoryDao.getByArticleAndLocation(articleUuid, toLocationUuid)
                ?: InventoryEntity(articleUuid, toLocationUuid, 0.0, timestamp)
            upsertInventory(inventory.copy(currentQuantity = inventory.currentQuantity + quantity, lastMovementAt = timestamp))
        }
    }

    private suspend fun upsertInventory(inventory: InventoryEntity) {
        val existing = inventoryDao.getByArticleAndLocation(inventory.articleUuid, inventory.locationUuid)
        if (existing != null) {
            inventoryDao.update(inventory)
        } else {
            inventoryDao.insert(inventory)
        }
    }

    /**
     * Recupera tutti i movimenti
     */
    override suspend fun getAllMovements(): Result<List<Movement>> {
        return try {
            val entities = movementDao.getAllMovements()
            val movements = entities.map { movementMapper.toDomain(it) }
            Result.success(movements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMovementByUuid(uuid: String): Result<Movement?> {
        return try {
            val entity = movementDao.getById(uuid)
            val movement = entity?.let { movementMapper.toDomain(it) }
            Result.success(movement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeMovementsByArticle(articleUuid: String): Flow<List<Movement>> {
        return movementDao.observeByArticleUuid(articleUuid).map { entities ->
            movementMapper.toDomainList(entities)
        }
    }

    override suspend fun getMovementsByArticle(articleUuid: String): Result<List<Movement>> {
        return try {
            val entities = movementDao.getByArticleUuid(articleUuid)
            val movements = movementMapper.toDomainList(entities)
            Result.success(movements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera ultimi N movimenti ordinati per data (desc)
     */
    override suspend fun getRecentMovements(limit: Int): Result<List<Movement>> {
        return try {
            val entities = movementDao.getRecentMovements(limit)
            val movements = entities.map { movementMapper.toDomain(it) }
            Result.success(movements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllMovements(): Flow<List<Movement>> {
        return movementDao.observeAll().map { entities ->
            movementMapper.toDomainList(entities)
        }
    }

    override fun observeMovementsByType(type: MovementType): Flow<List<Movement>> {
        return movementDao.observeByType(type).map { entities ->
            movementMapper.toDomainList(entities)
        }
    }

    override fun observeMovementsByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<Movement>> {
        return movementDao.observeByDateRange(startTimestamp, endTimestamp).map { entities ->
            movementMapper.toDomainList(entities)
        }
    }

    override suspend fun deleteMovement(uuid: String): Result<Unit> {
        return try {
            val movement = movementDao.getById(uuid)
            if (movement != null) {
                movementDao.delete(movement)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Movement not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

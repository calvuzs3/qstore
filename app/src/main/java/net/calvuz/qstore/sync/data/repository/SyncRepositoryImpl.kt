package net.calvuz.qstore.sync.data.repository

import android.util.Base64
import net.calvuz.qstore.app.data.local.database.ArticleDao
import net.calvuz.qstore.app.data.local.database.ArticleImageDao
import net.calvuz.qstore.app.data.local.database.ArticleLocationThresholdDao
import net.calvuz.qstore.app.data.local.database.LocationDao
import net.calvuz.qstore.app.data.local.database.MovementDao
import net.calvuz.qstore.app.data.local.entity.ArticleCategoryEntity
import net.calvuz.qstore.app.data.local.entity.ArticleEntity
import net.calvuz.qstore.app.data.local.entity.ArticleImageEntity
import net.calvuz.qstore.app.data.local.entity.ArticleLocationThresholdEntity
import net.calvuz.qstore.app.data.local.entity.LocationEntity
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.repository.MovementRepository
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import net.calvuz.qstore.categories.data.local.ArticleCategoryDao
import net.calvuz.qstore.sync.data.SyncLocalStore
import net.calvuz.qstore.sync.data.remote.SyncApi
import net.calvuz.qstore.sync.data.remote.dto.ArticleCategoryDto
import net.calvuz.qstore.sync.data.remote.dto.ArticleDto
import net.calvuz.qstore.sync.data.remote.dto.ArticleImageDto
import net.calvuz.qstore.sync.data.remote.dto.ArticleLocationThresholdDto
import net.calvuz.qstore.sync.data.remote.dto.LocationDto
import net.calvuz.qstore.sync.data.remote.dto.MovementDto
import net.calvuz.qstore.sync.data.remote.dto.SyncPullResponse
import net.calvuz.qstore.sync.data.remote.dto.SyncPushRequest
import net.calvuz.qstore.sync.domain.model.SyncException
import net.calvuz.qstore.sync.domain.model.SyncSummary
import net.calvuz.qstore.sync.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Sincronizzazione manuale: push delle righe locali modificate dopo l'ultimo cursore, poi
 * pull delle modifiche altrui, con upsert last-write-wins (stessa disciplina del server —
 * vedi quickstore-server/CLAUDE.md sezione 6).
 *
 * Limiti noti di questa prima versione (nessun WebSocket/WorkManager ancora):
 * - Le entity locali (article_categories, articles, locations,
 *   article_location_thresholds, article_images) non hanno un flag isDeleted — le
 *   cancellazioni locali sono hard-delete e non vengono propagate al server; una
 *   cancellazione remota invece viene applicata localmente (isDeleted=true in arrivo ->
 *   riga cancellata qui).
 * - article_images non ha updated_at lato client: si usa created_at come proxy.
 */
class SyncRepositoryImpl @Inject constructor(
    private val syncApi: SyncApi,
    private val syncLocalStore: SyncLocalStore,
    private val authRepository: AuthRepository,
    private val articleCategoryDao: ArticleCategoryDao,
    private val articleDao: ArticleDao,
    private val locationDao: LocationDao,
    private val articleLocationThresholdDao: ArticleLocationThresholdDao,
    private val movementDao: MovementDao,
    private val movementRepository: MovementRepository,
    private val articleImageDao: ArticleImageDao
) : SyncRepository {

    override suspend fun syncNow(): Result<SyncSummary> {
        return try {
            val session = authRepository.observeSession().first()
                ?: throw SyncException("Devi accedere (Impostazioni > Account) prima di sincronizzare")

            val since = syncLocalStore.getSince()
            val deviceId = syncLocalStore.getDeviceId()

            val pushRequest = buildPushRequest(since, deviceId, session.userId)
            val pushResponse = if (hasAnyRows(pushRequest)) syncApi.push(pushRequest) else null

            val pullResponse = syncApi.pull(since)
            applyPullResponse(pullResponse)
            syncLocalStore.setSince(pullResponse.serverTimestamp)

            Result.success(
                SyncSummary(
                    pushedCount = pushResponse?.acceptedIds?.size ?: 0,
                    rejectedCount = pushResponse?.rejectedIds?.size ?: 0,
                    pulledCount = pullResponse.articleCategories.size + pullResponse.locations.size +
                        pullResponse.articles.size + pullResponse.articleLocationThresholds.size +
                        pullResponse.movements.size + pullResponse.articleImages.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hasAnyRows(request: SyncPushRequest): Boolean =
        request.articleCategories.isNotEmpty() || request.locations.isNotEmpty() ||
            request.articles.isNotEmpty() || request.articleLocationThresholds.isNotEmpty() ||
            request.movements.isNotEmpty() || request.articleImages.isNotEmpty()

    // ===== PUSH =====

    private suspend fun buildPushRequest(since: Long, deviceId: String, fallbackUserId: String): SyncPushRequest {
        return SyncPushRequest(
            deviceId = deviceId,
            articleCategories = articleCategoryDao.getUpdatedSince(since).map { it.toDto() },
            locations = locationDao.getUpdatedSince(since).map { it.toDto() },
            articles = articleDao.getUpdatedSince(since).map { it.toDto() },
            articleLocationThresholds = articleLocationThresholdDao.getUpdatedSince(since).map { it.toDto() },
            movements = movementDao.getCreatedSince(since).map { it.toDto(fallbackUserId) },
            articleImages = articleImageDao.getUpdatedSince(since).map { it.toDto() }
        )
    }

    private fun ArticleCategoryEntity.toDto() = ArticleCategoryDto(
        id = uuid, name = name, description = description, notes = notes,
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = false
    )

    private fun ArticleEntity.toDto() = ArticleDto(
        id = uuid, name = name, description = description, categoryId = categoryId,
        unitOfMeasure = unitOfMeasure, reorderLevel = reorderLevel, notes = notes,
        codeOem = codeOEM, codeErp = codeERP, codeBm = codeBM,
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = false
    )

    private fun LocationEntity.toDto() = LocationDto(
        id = uuid, name = name, notes = notes, createdAt = createdAt, updatedAt = updatedAt, isDeleted = false
    )

    private fun ArticleLocationThresholdEntity.toDto() = ArticleLocationThresholdDto(
        id = uuid, articleId = articleUuid, locationId = locationUuid, reorderLevel = reorderLevel,
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = false
    )

    private fun net.calvuz.qstore.app.data.local.entity.MovementEntity.toDto(fallbackUserId: String) = MovementDto(
        id = id, articleId = articleUuid, type = type.name,
        fromLocationId = fromLocationUuid, toLocationId = toLocationUuid,
        quantity = quantity, notes = notes, createdBy = createdBy ?: fallbackUserId, createdAt = createdAt
    )

    private fun ArticleImageEntity.toDto() = ArticleImageDto(
        id = uuid, articleId = articleUuid, imagePath = imagePath,
        featuresData = Base64.encodeToString(featuresData, Base64.NO_WRAP),
        createdAt = createdAt, updatedAt = createdAt, isDeleted = false
    )

    // ===== PULL — upsert LWW, ordine di dipendenza: categories -> locations -> articles ->
    //              thresholds -> movements -> images (stesso ordine del server) =====

    private suspend fun applyPullResponse(response: SyncPullResponse) {
        response.articleCategories.forEach { upsertCategory(it) }
        response.locations.forEach { upsertLocation(it) }
        response.articles.forEach { upsertArticle(it) }
        response.articleLocationThresholds.forEach { upsertThreshold(it) }
        response.movements.forEach { ingestMovement(it) }
        response.articleImages.forEach { upsertImage(it) }
    }

    private suspend fun upsertCategory(dto: ArticleCategoryDto) {
        val existing = articleCategoryDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleCategoryDao.delete(it) }
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) return // LWW: scarta se non più recente
        val entity = ArticleCategoryEntity(
            uuid = dto.id, name = dto.name, description = dto.description, notes = dto.notes,
            createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) articleCategoryDao.update(entity) else articleCategoryDao.insert(entity)
    }

    private suspend fun upsertLocation(dto: LocationDto) {
        val existing = locationDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { locationDao.delete(it) }
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) return
        val entity = LocationEntity(
            uuid = dto.id, name = dto.name, notes = dto.notes,
            createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) locationDao.update(entity) else locationDao.insert(entity)
    }

    private suspend fun upsertArticle(dto: ArticleDto) {
        val existing = articleDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleDao.delete(it) }
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) return
        val entity = ArticleEntity(
            uuid = dto.id, name = dto.name, description = dto.description, categoryId = dto.categoryId,
            unitOfMeasure = dto.unitOfMeasure, reorderLevel = dto.reorderLevel, notes = dto.notes,
            codeOEM = dto.codeOem, codeERP = dto.codeErp, codeBM = dto.codeBm,
            createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) articleDao.update(entity) else articleDao.insert(entity)
    }

    private suspend fun upsertThreshold(dto: ArticleLocationThresholdDto) {
        val existing = articleLocationThresholdDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleLocationThresholdDao.delete(it) }
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) return
        val entity = ArticleLocationThresholdEntity(
            uuid = dto.id, articleUuid = dto.articleId, locationUuid = dto.locationId,
            reorderLevel = dto.reorderLevel, createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) articleLocationThresholdDao.update(entity) else articleLocationThresholdDao.insert(entity)
    }

    private suspend fun ingestMovement(dto: MovementDto) {
        movementRepository.ingestPulledMovement(
            Movement(
                id = dto.id,
                articleUuid = dto.articleId,
                type = MovementType.valueOf(dto.type),
                fromLocationUuid = dto.fromLocationId,
                toLocationUuid = dto.toLocationId,
                quantity = dto.quantity,
                notes = dto.notes,
                createdAt = dto.createdAt,
                createdBy = dto.createdBy
            )
        )
    }

    private suspend fun upsertImage(dto: ArticleImageDto) {
        val existing = articleImageDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleImageDao.delete(it) }
            return
        }
        if (existing != null && dto.updatedAt <= existing.createdAt) return // niente updatedAt locale, confronto su createdAt
        val entity = ArticleImageEntity(
            uuid = dto.id, articleUuid = dto.articleId, imagePath = dto.imagePath,
            featuresData = Base64.decode(dto.featuresData, Base64.NO_WRAP),
            createdAt = existing?.createdAt ?: dto.createdAt
        )
        if (existing != null) articleImageDao.insertOrReplace(entity) else articleImageDao.insert(entity)
    }
}

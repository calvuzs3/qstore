package net.calvuz.qstore.sync.data.repository

import android.content.Context
import android.util.Base64
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qstore.app.data.local.database.ArticleDao
import net.calvuz.qstore.app.data.local.database.ArticleImageDao
import net.calvuz.qstore.app.data.local.database.ArticleLocationThresholdDao
import net.calvuz.qstore.app.data.local.database.LocationDao
import net.calvuz.qstore.app.data.local.database.MovementDao
import net.calvuz.qstore.app.data.local.storage.ImageStorageManager
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
import net.calvuz.qstore.sync.data.worker.ImageTransferWorker
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

private val log = Timber.tag("Sync")

/**
 * Sincronizzazione manuale: push delle righe locali modificate dopo l'ultimo cursore, poi
 * pull delle modifiche altrui, con upsert last-write-wins (stessa disciplina del server —
 * vedi quickstore-server/CLAUDE.md sezione 6).
 *
 * Cancellazioni: le entity locali (article_categories, articles, locations,
 * article_location_thresholds, article_images) hanno un flag isDeleted — una cancellazione
 * locale diventa un soft-delete (vedi i rispettivi repository), viene raccolta dalla stessa
 * query getUpdatedSince già usata per gli update normali (una cancellazione è concettualmente
 * solo un altro update) e propagata al server qui. Una cancellazione remota (isDeleted=true
 * in arrivo) viene invece applicata come DELETE fisico locale — nessun bisogno di tenere un
 * tombstone locale per qualcosa che il server ci ha già confermato cancellato. `movements`
 * resta escluso deliberatamente: è un log append-only, non ha senso "cancellarlo" così.
 *
 * Limiti noti di questa prima versione (nessun WebSocket/WorkManager ancora):
 * - No WebSocket client, no periodic background sync — solo push/pull manuale.
 */
class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncApi: SyncApi,
    private val syncLocalStore: SyncLocalStore,
    private val authRepository: AuthRepository,
    private val articleCategoryDao: ArticleCategoryDao,
    private val articleDao: ArticleDao,
    private val locationDao: LocationDao,
    private val articleLocationThresholdDao: ArticleLocationThresholdDao,
    private val movementDao: MovementDao,
    private val movementRepository: MovementRepository,
    private val articleImageDao: ArticleImageDao,
    private val imageStorageManager: ImageStorageManager
) : SyncRepository {

    override suspend fun syncNow(): Result<SyncSummary> {
        return try {
            val session = authRepository.observeSession().first()
                ?: throw SyncException("Devi accedere (Impostazioni > Account) prima di sincronizzare")

            val since = syncLocalStore.getSince()
            val deviceId = syncLocalStore.getDeviceId()
            log.i("syncNow start: since=$since deviceId=$deviceId org=${session.orgName}")

            val pushRequest = buildPushRequest(since, deviceId, session.userId)
            log.d(
                "push payload: categories=%d locations=%d articles=%d thresholds=%d movements=%d images=%d",
                pushRequest.articleCategories.size, pushRequest.locations.size, pushRequest.articles.size,
                pushRequest.articleLocationThresholds.size, pushRequest.movements.size, pushRequest.articleImages.size
            )
            val pushResponse = if (hasAnyRows(pushRequest)) syncApi.push(pushRequest) else null
            if (pushResponse != null) {
                log.i("push result: accepted=${pushResponse.acceptedIds.size} rejected=${pushResponse.rejectedIds.size}")
                pushResponse.rejectedIds.forEach { log.w("push rejected id=${it.id} reason=${it.reason}") }
            } else {
                log.d("push skipped: nothing to send")
            }

            val pullResponse = syncApi.pull(since)
            log.d(
                "pull payload: categories=%d locations=%d articles=%d thresholds=%d movements=%d images=%d serverTimestamp=%d",
                pullResponse.articleCategories.size, pullResponse.locations.size, pullResponse.articles.size,
                pullResponse.articleLocationThresholds.size, pullResponse.movements.size,
                pullResponse.articleImages.size, pullResponse.serverTimestamp
            )
            val failedMovements = applyPullResponse(pullResponse)
            syncLocalStore.setSince(pullResponse.serverTimestamp)
            log.i("syncNow done: failedMovements=$failedMovements")

            scheduleImageTransfer()

            Result.success(
                SyncSummary(
                    pushedCount = pushResponse?.acceptedIds?.size ?: 0,
                    rejectedCount = pushResponse?.rejectedIds?.size ?: 0,
                    pulledCount = pullResponse.articleCategories.size + pullResponse.locations.size +
                        pullResponse.articles.size + pullResponse.articleLocationThresholds.size +
                        pullResponse.movements.size + pullResponse.articleImages.size,
                    failedMovements = failedMovements
                )
            )
        } catch (e: Exception) {
            log.e(e, "syncNow failed")
            Result.failure(e)
        }
    }

    /**
     * Il trasferimento foto (JPEG reali) è separato dal sync veloce di metadati appena
     * concluso — può essere pesante, gira in background con notifica di avanzamento (vedi
     * ImageTransferWorker). Sempre accodato dopo una sync riuscita: il worker stesso
     * ritorna subito se non c'è nulla da caricare/scaricare, controllarlo qui duplicherebbe
     * la stessa query.
     */
    private suspend fun scheduleImageTransfer() {
        val allowMetered = syncLocalStore.observeAllowMeteredNetworkForImages().first()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()
        val request = OneTimeWorkRequestBuilder<ImageTransferWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ImageTransferWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
        log.d("ImageTransferWorker enqueued (allowMetered=$allowMetered)")
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
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun ArticleEntity.toDto() = ArticleDto(
        id = uuid, name = name, description = description, categoryId = categoryId,
        unitOfMeasure = unitOfMeasure, reorderLevel = reorderLevel, notes = notes,
        codeOem = codeOEM, codeErp = codeERP, codeBm = codeBM,
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun LocationEntity.toDto() = LocationDto(
        id = uuid, name = name, notes = notes, createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun ArticleLocationThresholdEntity.toDto() = ArticleLocationThresholdDto(
        id = uuid, articleId = articleUuid, locationId = locationUuid, reorderLevel = reorderLevel,
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
    )

    private fun net.calvuz.qstore.app.data.local.entity.MovementEntity.toDto(fallbackUserId: String) = MovementDto(
        id = id, articleId = articleUuid, type = type.name,
        fromLocationId = fromLocationUuid, toLocationId = toLocationUuid,
        quantity = quantity, notes = notes, createdBy = createdBy ?: fallbackUserId, createdAt = createdAt
    )

    private fun ArticleImageEntity.toDto() = ArticleImageDto(
        id = uuid, articleId = articleUuid, imagePath = imagePath,
        featuresData = Base64.encodeToString(featuresData, Base64.NO_WRAP),
        createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
    )

    // ===== PULL — upsert LWW, ordine di dipendenza: categories -> locations -> articles ->
    //              thresholds -> movements -> images (stesso ordine del server) =====

    /** @return numero di movimenti che non sono stati applicati con successo. */
    private suspend fun applyPullResponse(response: SyncPullResponse): Int {
        response.articleCategories.forEach { upsertCategory(it) }
        response.locations.forEach { upsertLocation(it) }
        response.articles.forEach { upsertArticle(it) }
        response.articleLocationThresholds.forEach { upsertThreshold(it) }
        val failedMovements = response.movements.count { !ingestMovement(it) }
        response.articleImages.forEach { upsertImage(it) }
        return failedMovements
    }

    private suspend fun upsertCategory(dto: ArticleCategoryDto) {
        val existing = articleCategoryDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleCategoryDao.delete(it) }
            log.d("category ${dto.id} deleted (remoto)")
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) {
            log.v("category ${dto.id} skip (stale, local updatedAt=${existing.updatedAt} >= remote ${dto.updatedAt})")
            return
        }
        val entity = ArticleCategoryEntity(
            uuid = dto.id, name = dto.name, description = dto.description, notes = dto.notes,
            createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) articleCategoryDao.update(entity) else articleCategoryDao.insert(entity)
        log.d("category ${dto.id} '${dto.name}' ${if (existing != null) "updated" else "inserted"}")
    }

    private suspend fun upsertLocation(dto: LocationDto) {
        val existing = locationDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { locationDao.delete(it) }
            log.d("location ${dto.id} deleted (remoto)")
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) {
            log.v("location ${dto.id} skip (stale)")
            return
        }
        val entity = LocationEntity(
            uuid = dto.id, name = dto.name, notes = dto.notes,
            createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) locationDao.update(entity) else locationDao.insert(entity)
        log.d("location ${dto.id} '${dto.name}' ${if (existing != null) "updated" else "inserted"}")
    }

    private suspend fun upsertArticle(dto: ArticleDto) {
        val existing = articleDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleDao.delete(it) }
            log.d("article ${dto.id} deleted (remoto)")
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) {
            log.v("article ${dto.id} skip (stale)")
            return
        }
        val entity = ArticleEntity(
            uuid = dto.id, name = dto.name, description = dto.description, categoryId = dto.categoryId,
            unitOfMeasure = dto.unitOfMeasure, reorderLevel = dto.reorderLevel, notes = dto.notes,
            codeOEM = dto.codeOem, codeERP = dto.codeErp, codeBM = dto.codeBm,
            createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) articleDao.update(entity) else articleDao.insert(entity)
        log.d("article ${dto.id} '${dto.name}' ${if (existing != null) "updated" else "inserted"}")
    }

    private suspend fun upsertThreshold(dto: ArticleLocationThresholdDto) {
        val existing = articleLocationThresholdDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            existing?.let { articleLocationThresholdDao.delete(it) }
            log.d("threshold ${dto.id} deleted (remoto)")
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) {
            log.v("threshold ${dto.id} skip (stale)")
            return
        }
        val entity = ArticleLocationThresholdEntity(
            uuid = dto.id, articleUuid = dto.articleId, locationUuid = dto.locationId,
            reorderLevel = dto.reorderLevel, createdAt = existing?.createdAt ?: dto.createdAt, updatedAt = dto.updatedAt
        )
        if (existing != null) articleLocationThresholdDao.update(entity) else articleLocationThresholdDao.insert(entity)
        log.d("threshold ${dto.id} ${if (existing != null) "updated" else "inserted"}")
    }

    /**
     * @return true se applicato con successo. Il Result di ingestPulledMovement NON va mai
     * scartato: un fallimento silenzioso qui (es. vincolo FK su un articolo/ubicazione non
     * ancora presente) lascia l'inventario locale disallineato dallo storico movimenti senza
     * che il sync riporti alcun errore — bug reale osservato: giacenze sempre a 0 dopo una
     * pull "da zero" perché i movimenti fallivano silenziosamente.
     */
    private suspend fun ingestMovement(dto: MovementDto): Boolean {
        val movement = Movement(
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
        return movementRepository.ingestPulledMovement(movement)
            .onSuccess { log.d("movement ${dto.id} article=${dto.articleId} type=${dto.type} ingested") }
            .onFailure { log.e(it, "movement ${dto.id} article=${dto.articleId} type=${dto.type} FAILED to ingest") }
            .isSuccess
    }

    private suspend fun upsertImage(dto: ArticleImageDto) {
        val existing = articleImageDao.getByUuid(dto.id)
        if (dto.isDeleted) {
            if (existing != null) {
                imageStorageManager.deleteImage(existing.imagePath)
                articleImageDao.delete(existing)
            }
            log.d("image ${dto.id} deleted (remoto)")
            return
        }
        if (existing != null && dto.updatedAt <= existing.updatedAt) {
            log.v("image ${dto.id} skip (stale)")
            return
        }
        val entity = ArticleImageEntity(
            uuid = dto.id, articleUuid = dto.articleId, imagePath = dto.imagePath,
            featuresData = Base64.decode(dto.featuresData, Base64.NO_WRAP),
            createdAt = existing?.createdAt ?: dto.createdAt,
            updatedAt = dto.updatedAt,
            isUploaded = true // arrivata via pull: per definizione già sul server
        )
        if (existing != null) articleImageDao.insertOrReplace(entity) else articleImageDao.insert(entity)
        log.d("image ${dto.id} article=${dto.articleId} ${if (existing != null) "updated" else "inserted"} (solo descrittori, JPEG da scaricare a parte)")
    }
}

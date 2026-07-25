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
import net.calvuz.qstore.app.data.local.database.QuickStoreDatabase
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
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import net.calvuz.qstore.categories.data.local.ArticleCategoryDao
import net.calvuz.qstore.sync.data.SyncLocalStore
import net.calvuz.qstore.sync.data.remote.SyncApi
import net.calvuz.qstore.shared.dto.ArticleCategoryDto
import net.calvuz.qstore.shared.dto.ArticleDto
import net.calvuz.qstore.shared.dto.ArticleImageDto
import net.calvuz.qstore.shared.dto.ArticleLocationThresholdDto
import net.calvuz.qstore.shared.dto.LocationDto
import net.calvuz.qstore.shared.dto.MovementDto
import net.calvuz.qstore.shared.dto.SyncPullResponse
import net.calvuz.qstore.shared.dto.SyncPushRequest
import net.calvuz.qstore.sync.domain.model.BoundOrganization
import net.calvuz.qstore.sync.domain.model.PurgeSummary
import net.calvuz.qstore.sync.domain.model.SyncException
import net.calvuz.qstore.sync.domain.model.SyncSummary
import net.calvuz.qstore.sync.domain.repository.SyncRepository
import net.calvuz.qstore.sync.data.worker.ImageTransferWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
 * in arrivo) viene applicata anche localmente come soft-delete, MAI come DELETE fisico:
 * `articles` ha FK CASCADE da `inventory`/`movements`/`article_location_thresholds`/
 * `article_images`, quindi un DELETE fisico in pull si porterebbe via a cascata proprio lo
 * storico che il soft-delete locale (vedi ArticleRepositoryImpl.deleteArticle) preserva
 * deliberatamente — bug reale osservato: bastava che l'articolo appena cancellato tornasse
 * indietro nella pull dello stesso giro di sync perché i suoi movimenti sparissero anche sul
 * device che li aveva creati. La cancellazione fisica vera e propria è ora solo compito di
 * PurgeDeletedDataUseCase, esplicito e a richiesta dell'utente (vedi quella classe). `movements`
 * resta escluso deliberatamente dal flag isDeleted: è un log append-only, non ha senso
 * "cancellarlo" così — la sua unica via di cancellazione è il CASCADE quando l'articolo viene
 * infine purgato per davvero.
 *
 * Limiti noti di questa prima versione (nessun WebSocket/WorkManager ancora):
 * - No WebSocket client, no periodic background sync — solo push/pull manuale.
 */
class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QuickStoreDatabase,
    private val syncApi: SyncApi,
    private val syncLocalStore: SyncLocalStore,
    private val authRepository: AuthRepository,
    private val backupRepository: BackupRepository,
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

            // Un device resta legato alla prima organizzazione con cui sincronizza con successo
            // — mai un DB locale che mescola dati di due org diverse (vedi la classe
            // BoundOrganization e SwitchOrganizationUseCase per come se ne esce). Controllo
            // fatto qui, prima di toccare rete o DB: se il device è legato a un'altra org,
            // il sync fallisce subito con un messaggio chiaro invece di mischiare dati.
            val boundOrgId = syncLocalStore.getBoundOrgId()
            if (boundOrgId != null && boundOrgId != session.orgId) {
                val boundOrgName = syncLocalStore.getBoundOrgName() ?: boundOrgId
                throw SyncException(
                    "Questo device ha dati sincronizzati con l'organizzazione '$boundOrgName'. " +
                        "Per usare '${session.orgName}' devi prima cambiare organizzazione da Impostazioni > Account."
                )
            }
            if (boundOrgId == null) {
                syncLocalStore.setBoundOrganization(session.orgId, session.orgName)
                log.i("device bound to organization '${session.orgName}' (${session.orgId})")
            }

            val sincePush = syncLocalStore.getSincePush()
            val sincePull = syncLocalStore.getSincePull()
            val deviceId = syncLocalStore.getDeviceId()
            log.i("syncNow start: sincePush=$sincePush sincePull=$sincePull deviceId=$deviceId org=${session.orgName}")

            // Catturato PRIMA di interrogare il DB (stesso motivo del server nella pull):
            // evita di perdere una riga modificata a metà costruzione del payload, che
            // altrimenti sfuggirebbe sia a questo push che al prossimo. Orologio del
            // device, mai quello del server — vedi il commento su SyncLocalStore.
            val pushTimestamp = System.currentTimeMillis()
            val pushRequest = buildPushRequest(sincePush, deviceId, session.userId)
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
            // Avanza solo se siamo arrivati vivi fin qui (altrimenti l'eccezione sarebbe già
            // stata rilanciata sopra, uscendo dal try prima di questa riga).
            syncLocalStore.setSincePush(pushTimestamp)

            val pullResponse = syncApi.pull(sincePull)
            log.d(
                "pull payload: categories=%d locations=%d articles=%d thresholds=%d movements=%d images=%d serverTimestamp=%d",
                pullResponse.articleCategories.size, pullResponse.locations.size, pullResponse.articles.size,
                pullResponse.articleLocationThresholds.size, pullResponse.movements.size,
                pullResponse.articleImages.size, pullResponse.serverTimestamp
            )
            val failedMovements = applyPullResponse(pullResponse)
            syncLocalStore.setSincePull(pullResponse.serverTimestamp)
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
     * DELETE fisico vero e proprio dei tombstone (is_deleted=1) più vecchi della retention
     * richiesta — l'unico punto di tutto il sync path dove una riga sparisce davvero dal
     * device, mai in risposta a un semplice giro di sync (vedi il commento in cima al file).
     *
     * Il cutoff effettivo è il minimo tra "ora meno retention" e `sincePush`: non basta che
     * una cancellazione sia vecchia, deve anche essere già stata comunicata al server,
     * altrimenti la si perderebbe localmente senza che sia mai stata propagata (un device
     * che ha soft-eliminato qualcosa e poi non ha più sincronizzato non deve poterla purgare
     * da solo). Un device senza alcuna sessione/sync attiva ha sincePush=0, quindi il cutoff
     * resta 0 e la purge non elimina nulla — comportamento corretto, non un bug: i tombstone
     * restano invisibili in ogni lista/ricerca comunque, l'unico costo è spazio su disco.
     *
     * Ordine: articles prima di categories, perché il CASCADE su un articolo purgato porta
     * via anche eventuali categorie che restavano referenziate solo da lui, rendendole a loro
     * volta eleggibili nello stesso giro.
     */
    override suspend fun purgeDeletedData(retentionMillis: Long): Result<PurgeSummary> {
        return try {
            val sincePush = syncLocalStore.getSincePush()
            val cutoff = minOf(System.currentTimeMillis() - retentionMillis, sincePush)
            log.i("purgeDeletedData: retentionMillis=$retentionMillis sincePush=$sincePush cutoff=$cutoff")

            // I JPEG vanno cancellati ORA, prima delle righe: nessun soft-delete (locale o
            // via sync) tocca mai il file fisico, resta sul device apposta per non perderlo
            // prima di un eventuale restore. Se aspettassimo il DELETE di articleDao qui
            // sotto, il CASCADE farebbe sparire la riga article_images senza eseguire codice
            // Kotlin — il path andrebbe perso per sempre senza che il file venga mai rimosso.
            val purgeableImages = articleImageDao.getPurgeable(cutoff)
            purgeableImages.forEach { image -> imageStorageManager.deleteImage(image.imagePath) }

            val articlesPurged = articleDao.purgeDeleted(cutoff)
            // Ripulisce solo le righe rimaste (cancellazioni indipendenti da un articolo
            // ancora vivo): quelle del CASCADE sopra sono già sparite, il loro file è già
            // stato cancellato nel loop precedente.
            articleImageDao.purgeDeleted(cutoff)
            val categoriesPurged = articleCategoryDao.purgeDeleted(cutoff)

            val summary = PurgeSummary(articlesPurged, purgeableImages.size, categoriesPurged)
            log.i("purgeDeletedData done: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            log.e(e, "purgeDeletedData failed")
            Result.failure(e)
        }
    }

    override suspend fun getBoundOrganization(): BoundOrganization? {
        val orgId = syncLocalStore.getBoundOrgId() ?: return null
        return BoundOrganization(orgId, syncLocalStore.getBoundOrgName() ?: orgId)
    }

    /**
     * Wipe totale e irreversibile: DB Room + JPEG su disco + cursori di sync, per liberare il
     * device da un'organizzazione e permettergli di legarsi a un'altra (vedi
     * SwitchOrganizationUseCase, mai chiamata senza conferma esplicita dell'utente). `deviceId`
     * NON viene toccato: identifica il device fisico, non ha nulla a che fare con l'org.
     *
     * Backup di sicurezza automatico prima del wipe, stessa logica precauzionale già usata da
     * BackupRepositoryImpl prima di un restore — "non blocchiamo se fallisce, è solo
     * precauzionale" (limite noto: il formato di backup non porta ancora `locations`, vedi
     * CLAUDE.md "Backup Format").
     */
    override suspend fun switchOrganization(): Result<Unit> {
        return try {
            log.w("switchOrganization: wiping all local data (DB + images) to leave the current organization")
            try {
                backupRepository.createBackupSync()
            } catch (e: Exception) {
                log.w(e, "switchOrganization: safety backup failed, proceeding anyway (precauzionale)")
            }

            // clearAllTables() è una chiamata Room bloccante (non una suspend fun generata),
            // va spostata fuori dal thread del chiamante esplicitamente.
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                imageStorageManager.deleteAllImages()
            }
            syncLocalStore.resetSyncCursors()
            syncLocalStore.clearBoundOrganization()

            log.i("switchOrganization done")
            Result.success(Unit)
        } catch (e: Exception) {
            log.e(e, "switchOrganization failed")
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
            existing?.let { articleCategoryDao.markDeleted(dto.id, dto.updatedAt) }
            log.d("category ${dto.id} soft-deleted (remoto)")
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
            existing?.let { locationDao.markDeleted(dto.id, dto.updatedAt) }
            log.d("location ${dto.id} soft-deleted (remoto)")
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
            existing?.let { articleDao.markDeleted(dto.id, dto.updatedAt) }
            log.d("article ${dto.id} soft-deleted (remoto)")
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
            existing?.let { articleLocationThresholdDao.markDeleted(dto.id, dto.updatedAt) }
            log.d("threshold ${dto.id} soft-deleted (remoto)")
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
            // JPEG NON toccato qui: resta sul device finché non arriva un purge esplicito
            // (PurgeDeletedDataUseCase) — stessa ragione della cancellazione locale, vedi
            // ArticleRepositoryImpl.deleteArticle.
            existing?.let { articleImageDao.markDeleted(dto.id, dto.updatedAt) }
            log.d("image ${dto.id} soft-deleted (remoto)")
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

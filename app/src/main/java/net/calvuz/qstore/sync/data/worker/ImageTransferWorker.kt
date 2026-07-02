package net.calvuz.qstore.sync.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.calvuz.qstore.app.data.local.database.ArticleImageDao
import net.calvuz.qstore.app.data.local.storage.ImageStorageManager
import net.calvuz.qstore.sync.data.remote.ImagesApi
import timber.log.Timber

private val log = Timber.tag("Sync")

/**
 * Trasferisce i JPEG reali delle foto articolo: upload di quelle scattate su questo
 * device e non ancora sul server (is_uploaded=false), download di quelle il cui
 * metadato è arrivato via /sync/pull ma il cui file non esiste ancora su questo device.
 * Separato dal sync veloce di metadati (SyncRepositoryImpl.syncNow) perché il carico può
 * essere pesante — gira come foreground service con notifica di avanzamento, così
 * l'utente sa che è in corso e Android non lo termina per pressione di memoria.
 */
@HiltWorker
class ImageTransferWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val articleImageDao: ArticleImageDao,
    private val imagesApi: ImagesApi,
    private val imageStorageManager: ImageStorageManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingUpload = articleImageDao.getPendingUpload()
        val pendingDownload = articleImageDao.getAll().filter { !imageStorageManager.imageExists(it.imagePath) }
        val total = pendingUpload.size + pendingDownload.size

        log.i("ImageTransferWorker start: toUpload=${pendingUpload.size} toDownload=${pendingDownload.size}")
        if (total == 0) return Result.success()

        setForeground(foregroundInfo(0, total))

        var done = 0
        var uploaded = 0
        var downloaded = 0
        var failed = 0

        for (image in pendingUpload) {
            try {
                val bytes = imageStorageManager.readImage(image.imagePath).getOrThrow()
                imagesApi.uploadImage(image.uuid, bytes)
                articleImageDao.markUploaded(image.uuid)
                uploaded++
                log.d("uploaded image ${image.uuid}")
            } catch (e: Exception) {
                log.e(e, "upload FAILED for image ${image.uuid}")
                failed++
            }
            done++
            setForeground(foregroundInfo(done, total))
        }

        for (image in pendingDownload) {
            try {
                val bytes = imagesApi.downloadImage(image.uuid)
                imageStorageManager.writeImageAtPath(image.imagePath, bytes).getOrThrow()
                downloaded++
                log.d("downloaded image ${image.uuid}")
            } catch (e: Exception) {
                log.e(e, "download FAILED for image ${image.uuid}")
                failed++
            }
            done++
            setForeground(foregroundInfo(done, total))
        }

        log.i("ImageTransferWorker done: uploaded=$uploaded downloaded=$downloaded failed=$failed")
        showResultNotification(uploaded, downloaded, failed)
        return if (failed == 0) Result.success() else Result.retry()
    }

    private fun foregroundInfo(done: Int, total: Int): ForegroundInfo {
        val notification = buildImageTransferProgressNotification(applicationContext, done, total)
        return ForegroundInfo(
            IMAGE_TRANSFER_PROGRESS_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    @Suppress("MissingPermission") // richiesto e verificato all'ingresso in LoginScreen; se negato, notify() è un no-op silenzioso
    private fun showResultNotification(uploaded: Int, downloaded: Int, failed: Int) {
        val notification = buildImageTransferResultNotification(applicationContext, uploaded, downloaded, failed)
        NotificationManagerCompat.from(applicationContext).notify(IMAGE_TRANSFER_RESULT_NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "image_transfer"
    }
}

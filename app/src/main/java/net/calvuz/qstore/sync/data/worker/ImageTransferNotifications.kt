package net.calvuz.qstore.sync.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import net.calvuz.qstore.R

const val IMAGE_TRANSFER_CHANNEL_ID = "image_transfer"
const val IMAGE_TRANSFER_PROGRESS_NOTIFICATION_ID = 4201
const val IMAGE_TRANSFER_RESULT_NOTIFICATION_ID = 4202

/** Va chiamata una volta all'avvio dell'app (QuickStoreApplication.onCreate). */
fun createImageTransferNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        IMAGE_TRANSFER_CHANNEL_ID,
        "Trasferimento foto",
        NotificationManager.IMPORTANCE_LOW // niente suono/vibrazione, è un lavoro di sync
    ).apply {
        description = "Avanzamento upload/download delle foto articoli durante la sincronizzazione"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

fun buildImageTransferProgressNotification(
    context: Context,
    done: Int,
    total: Int
): android.app.Notification {
    return NotificationCompat.Builder(context, IMAGE_TRANSFER_CHANNEL_ID)
        .setContentTitle("Sincronizzazione foto")
        .setContentText("$done di $total")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setProgress(total, done, false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()
}

fun buildImageTransferResultNotification(
    context: Context,
    uploaded: Int,
    downloaded: Int,
    failed: Int
): android.app.Notification {
    val text = buildString {
        append("$uploaded caricate, $downloaded scaricate")
        if (failed > 0) append(", $failed fallite")
    }
    return NotificationCompat.Builder(context, IMAGE_TRANSFER_CHANNEL_ID)
        .setContentTitle("Sincronizzazione foto completata")
        .setContentText(text)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setAutoCancel(true)
        .build()
}

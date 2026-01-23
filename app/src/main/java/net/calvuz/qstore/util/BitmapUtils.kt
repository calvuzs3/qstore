package net.calvuz.qstore.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/**
 * Utility per conversioni Bitmap
 */
object BitmapUtils {

    /**
     * Converte Bitmap in ByteArray (JPEG)
     *
     * @param bitmap Bitmap da convertire
     * @param quality Qualità JPEG (0-100), default 85
     * @return ByteArray dell'immagine JPEG
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Ridimensiona un Bitmap mantenendo aspect ratio
     *
     * @param bitmap Bitmap originale
     * @param maxSize Dimensione massima (lato più lungo)
     * @return Bitmap ridimensionato
     */
    fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
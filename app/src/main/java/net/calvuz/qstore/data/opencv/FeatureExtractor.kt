package net.calvuz.qstore.data.opencv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import androidx.core.graphics.scale

/**
 * Estrattore di features OpenCV migliorato per il riconoscimento oggetti
 * Utilizza preprocessing avanzato e configurazione ORB ottimizzata
 */
@Singleton
class FeatureExtractor @Inject constructor(
    private val openCVManager: OpenCVManager
) {
    private val orb: ORB by lazy {
        ORB.create(
            MAX_FEATURES,           // 1000 features per più dettagli
            1.2f,                   // Scale factor
            12,                     // Più livelli piramide per oggetti a diverse scale
            31,                     // Edge threshold
            0,                      // First level
            2,                      // WTA_K
            ORB.HARRIS_SCORE,       // Harris score per migliore qualità
            31,                     // Patch size
            15                      // Fast threshold più basso per più features
        )
    }

    /**
     * Estrae features da un'immagine con preprocessing avanzato
     */
    fun extractFeatures(imageData: ByteArray): Result<ByteArray> {
        if (!openCVManager.isInitialized()) {
            return Result.failure(IllegalStateException("OpenCV not initialized"))
        }

        return try {
            // Decodifica immagine
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return Result.failure(IllegalArgumentException("Invalid image data"))

            // Ridimensiona se troppo grande (migliora performance)
            val resizedBitmap = resizeBitmapIfNeeded(bitmap)

            val features = extractFeaturesFromBitmap(resizedBitmap)

            // Cleanup
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            bitmap.recycle()

            features

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Estrae features da un Bitmap con preprocessing completo
     */
    fun extractFeaturesFromBitmap(bitmap: Bitmap): Result<ByteArray> {
        if (!openCVManager.isInitialized()) {
            return Result.failure(IllegalStateException("OpenCV not initialized"))
        }

        var mat: Mat? = null
        var grayMat: Mat? = null
        var processedMat: Mat? = null
        var descriptors: Mat? = null

        return try {
            // Converti Bitmap a Mat
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            // Converti a grayscale
            grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // Preprocessing avanzato
            processedMat = preprocessImage(grayMat)

            // Estrai keypoints e descriptors
            val keypoints = MatOfKeyPoint()
            descriptors = Mat()

            orb.detectAndCompute(processedMat, Mat(), keypoints, descriptors)

            // Verifica qualità features
            val keypointsArray = keypoints.toArray()
            if (descriptors.empty() || keypointsArray.isEmpty()) {
                return Result.failure(IllegalStateException("No features detected in image"))
            }

            // Log per debug
            android.util.Log.d(TAG, "Extracted ${keypointsArray.size} keypoints, ${descriptors.rows()} descriptors")

            // Filtra features di alta qualità
            val filteredDescriptors = filterHighQualityFeatures(keypoints, descriptors)

            // Serializza descriptors
            val serialized = serializeMat(filteredDescriptors)
            Result.success(serialized)

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Cleanup
            mat?.release()
            grayMat?.release()
            processedMat?.release()
            descriptors?.release()
        }
    }

    /**
     * Preprocessing avanzato dell'immagine per migliorare feature detection
     */
    private fun preprocessImage(grayMat: Mat): Mat {
        val processed = Mat()

        // 1. Equalizzazione istogramma per migliorare contrasto
        Imgproc.equalizeHist(grayMat, processed)

        // 2. Filtro gaussiano leggero per ridurre noise
        val blurred = Mat()
        Imgproc.GaussianBlur(processed, blurred, Size(3.0, 3.0), 0.0)

        // 3. Sharpening per evidenziare bordi
        val kernel = Mat(3, 3, CvType.CV_32F)
        kernel.put(0, 0,
            0.0, -1.0, 0.0,
            -1.0, 5.0, -1.0,
            0.0, -1.0, 0.0
        )

        val sharpened = Mat()
        Imgproc.filter2D(blurred, sharpened, -1, kernel)

        // Cleanup
        processed.release()
        blurred.release()
        kernel.release()

        return sharpened
    }

    /**
     * Filtra solo le features di alta qualità basate su response score
     */
    private fun filterHighQualityFeatures(keypoints: MatOfKeyPoint, descriptors: Mat): Mat {
        val keypointsArray = keypoints.toArray()

        if (keypointsArray.size <= MIN_FEATURES) {
            return descriptors.clone()
        }

        // Ordina keypoints per response (qualità) decrescente
        val sortedIndices = keypointsArray
            .mapIndexed { index, keypoint -> index to keypoint.response }
            .sortedByDescending { it.second }
            .map { it.first }

        // Prendi solo le migliori N features
        val bestIndices = sortedIndices.take(min(MAX_FEATURES, keypointsArray.size))

        // Crea nuovo Mat con solo le migliori features
        val filteredDescriptors = Mat(bestIndices.size, descriptors.cols(), descriptors.type())

        bestIndices.forEachIndexed { newIndex, originalIndex ->
            val row = descriptors.row(originalIndex)
            row.copyTo(filteredDescriptors.row(newIndex))
            row.release()
        }

        android.util.Log.d(TAG, "Filtered from ${keypointsArray.size} to ${bestIndices.size} features")

        return filteredDescriptors
    }

    /**
     * Ridimensiona bitmap se troppo grande per migliorare performance
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = MAX_IMAGE_DIMENSION
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        android.util.Log.d(TAG, "Resizing from ${width}x${height} to ${newWidth}x${newHeight}")

        // return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * Deserializza descriptors da ByteArray a Mat
     */
    fun deserializeDescriptors(data: ByteArray): Result<Mat> {
        return try {
            val mat = deserializeMat(data)
            Result.success(mat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Serializza Mat in ByteArray
     */
    private fun serializeMat(mat: Mat): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // Scrivi dimensioni
        outputStream.write(intToBytes(mat.rows()))
        outputStream.write(intToBytes(mat.cols()))
        outputStream.write(intToBytes(mat.type()))

        // Scrivi dati
        val data = ByteArray(mat.total().toInt() * mat.elemSize().toInt())
        mat.get(0, 0, data)
        outputStream.write(data)

        return outputStream.toByteArray()
    }

    /**
     * Deserializza ByteArray in Mat
     */
    private fun deserializeMat(data: ByteArray): Mat {
        var offset = 0

        // Leggi dimensioni
        val rows = bytesToInt(data, offset)
        offset += 4
        val cols = bytesToInt(data, offset)
        offset += 4
        val type = bytesToInt(data, offset)
        offset += 4

        // Crea Mat
        val mat = Mat(rows, cols, type)

        // Leggi dati
        val matData = data.copyOfRange(offset, data.size)
        mat.put(0, 0, matData)

        return mat
    }

    /**
     * Converte Int in ByteArray (big endian)
     */
    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    /**
     * Converte ByteArray in Int (big endian)
     */
    private fun bytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    companion object {
        private const val TAG = "FeatureExtractor"
        private const val MAX_FEATURES = 1000        // Ridotto da 1000 per velocità
        private const val MIN_FEATURES = 50
        private const val MAX_IMAGE_DIMENSION = 800  // Ridotto da 800 per velocità
    }
}
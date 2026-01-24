package net.calvuz.qstore.app.data.opencv

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager per l'inizializzazione di OpenCV
 */
@Singleton
class OpenCVManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var isInitialized = false
    private var initializationAttempted = false
    private var lastError: String? = null

    // StateFlow per monitorare lo stato dell'inizializzazione
    private val _initializationState = MutableStateFlow(InitializationState.NOT_STARTED)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    // Callback per l'inizializzazione asincrona
    private val loaderCallback = object : BaseLoaderCallback(context) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "✅ OpenCV loaded successfully via callback")
                    isInitialized = true
                    lastError = null
                    _initializationState.value = InitializationState.SUCCESS
                    logOpenCVInfo()
                }
                else -> {
                    super.onManagerConnected(status)
                    val error = "OpenCV Manager connection failed with status: $status"
                    Log.e(TAG, "❌ $error")
                    lastError = error
                    _initializationState.value = InitializationState.ERROR
                }
            }
        }
    }

    /**
     * Inizializza OpenCV con fallback multipli
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "OpenCV already initialized")
            return@withContext true
        }

        if (initializationAttempted && _initializationState.value == InitializationState.ERROR) {
            Log.w(TAG, "Previous initialization failed, retrying...")
        }

        initializationAttempted = true
        _initializationState.value = InitializationState.IN_PROGRESS

        // Tentativo 1: Inizializzazione diretta (più veloce)
        try {
            Log.d(TAG, "🔄 Attempting direct OpenCV initialization...")
            val directSuccess = OpenCVLoader.initDebug()
            if (directSuccess) {
                isInitialized = true
                _initializationState.value = InitializationState.SUCCESS
                Log.i(TAG, "✅ OpenCV initialized successfully (direct)")
                logOpenCVInfo()
                return@withContext true
            } else {
                Log.w(TAG, "⚠️ Direct initialization failed, trying manager...")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Direct initialization exception: ${e.message}")
        }

        // Tentativo 2: OpenCV Manager (asincrono)
        try {
            Log.d(TAG, "🔄 Attempting OpenCV Manager initialization...")
            if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback)) {
                val error = "OpenCV Manager initialization call failed"
                Log.e(TAG, "❌ $error")
                lastError = error
                _initializationState.value = InitializationState.ERROR
                return@withContext false
            }

            // Attendi risultato asincrono (timeout dopo 10 secondi)
            var attempts = 0
            while (attempts < 100 && _initializationState.value == InitializationState.IN_PROGRESS) {
                kotlinx.coroutines.delay(100)
                attempts++
            }

            return@withContext isInitialized

        } catch (e: Exception) {
            val error = "OpenCV Manager initialization exception: ${e.message}"
            Log.e(TAG, "❌ $error", e)
            lastError = error
            _initializationState.value = InitializationState.ERROR
            return@withContext false
        }
    }
    /**
     * Verifica se OpenCV è stato inizializzato
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Ottiene l'ultimo errore di inizializzazione
     */
    fun getLastError(): String? = lastError

    /**
     * Ottiene informazioni dettagliate su OpenCV
     */
    fun getOpenCVInfo(): OpenCVInfo {
        return if (isInitialized) {
            try {
                OpenCVInfo(
                    isInitialized = true,
                    version = Core.getVersionString(),
                    buildInformation = Core.getBuildInformation(),
                    nativeLibraryDir = "No Native Lib Name",  //Core.getNativeLibraryName(),
                    optimizedCode = true,  // Core.useOptimized(),
                    numberOfCpus = Core.getNumberOfCPUs()
                )
            } catch (e: Exception) {
                OpenCVInfo(
                    isInitialized = true,
                    version = "Unknown",
                    error = "Error getting OpenCV info: ${e.message}"
                )
            }
        } else {
            OpenCVInfo(
                isInitialized = false,
                error = lastError ?: "Not initialized"
            )
        }
    }

    /**
     * Forza reinizializzazione
     */
    suspend fun reinitialize(): Boolean {
        Log.i(TAG, "🔄 Forcing OpenCV reinitialization...")
        isInitialized = false
        initializationAttempted = false
        lastError = null
        _initializationState.value = InitializationState.NOT_STARTED

        return initialize()
    }

    /**
     * Testa le funzionalità base di OpenCV
     */
    fun testOpenCVFunctionality(): TestResult {
        if (!isInitialized) {
            return TestResult(
                success = false,
                error = "OpenCV not initialized"
            )
        }

        return try {
            // Test 1: Creazione Mat
            val testMat = org.opencv.core.Mat(10, 10, org.opencv.core.CvType.CV_8UC1)
            val matTest = !testMat.empty()
            testMat.release()

            if (!matTest) {
                return TestResult(success = false, error = "Mat creation failed")
            }

            // Test 2: ORB creation
            val orb = org.opencv.features2d.ORB.create()
            val orbTest = orb != null

            if (!orbTest) {
                return TestResult(success = false, error = "ORB creation failed")
            }

            // Test 3: BFMatcher creation
            val matcher = org.opencv.features2d.BFMatcher.create()
            val matcherTest = matcher != null

            if (!matcherTest) {
                return TestResult(success = false, error = "BFMatcher creation failed")
            }

            TestResult(
                success = true,
                message = "All OpenCV functionality tests passed"
            )

        } catch (e: Exception) {
            TestResult(
                success = false,
                error = "OpenCV test exception: ${e.message}"
            )
        }
    }

    /**
     * Log informazioni dettagliate su OpenCV
     */
    private fun logOpenCVInfo() {
        try {
            Log.i(TAG, "📋 OpenCV Information:")
            Log.i(TAG, "   Version: ${Core.getVersionString()}")
            Log.i(TAG, "   Optimized: true")  //${Core.useOptimized()}")
            Log.i(TAG, "   CPUs: ${Core.getNumberOfCPUs()}")
            Log.i(TAG, "   Native Library: No Native Lib Name")  //${Core.getNativeLibraryName()}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Could not log OpenCV info: ${e.message}")
        }
    }

    /**
     * Stato dell'inizializzazione
     */
    enum class InitializationState {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCESS,
        ERROR
    }

    /**
     * Informazioni su OpenCV
     */
    data class OpenCVInfo(
        val isInitialized: Boolean,
        val version: String? = null,
        val buildInformation: String? = null,
        val nativeLibraryDir: String? = null,
        val optimizedCode: Boolean? = null,
        val numberOfCpus: Int? = null,
        val error: String? = null
    )

    /**
     * Risultato test funzionalità
     */
    data class TestResult(
        val success: Boolean,
        val message: String? = null,
        val error: String? = null
    )

    companion object {
        private const val TAG = "OpenCVManager"
    }
}
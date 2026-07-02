package net.calvuz.qstore

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.data.opencv.OpenCVManager
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class QuickStoreApplication : Application() {

    @Inject
    lateinit var openCVManager: OpenCVManager

    // Scope per operazioni asincrone nell'Application
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Log.d(TAG, "🚀 QuickStore Application Starting...")

        // Inizializza OpenCV in modo asincrono
        initializeOpenCV()
    }

    private fun initializeOpenCV() {
        Log.d(TAG, "🔄 Starting OpenCV initialization...")

        applicationScope.launch {
            try {
                val success = openCVManager.initialize()

                if (success) {
                    Log.i(TAG, "✅ OpenCV initialized successfully!")

                    // Test opzionale delle funzionalità
                    val testResult = openCVManager.testOpenCVFunctionality()
                    if (testResult.success) {
                        Log.i(TAG, "✅ OpenCV functionality test passed")
                    } else {
                        Log.w(TAG, "⚠️ OpenCV test failed: ${testResult.error}")
                    }

                } else {
                    Log.e(TAG, "❌ OpenCV initialization failed: ${openCVManager.getLastError()}")
                    // Qui potresti implementare una strategia di retry o fallback
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Exception during OpenCV initialization", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "🛑 QuickStore Application Terminating...")
    }

    companion object {
        private const val TAG = "QuickStoreApp"
    }
}
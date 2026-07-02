package net.calvuz.qstore

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.data.opencv.OpenCVManager
import net.calvuz.qstore.sync.data.worker.createImageTransferNotificationChannel
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class QuickStoreApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var openCVManager: OpenCVManager

    // Necessario perché i Worker (ImageTransferWorker) sono @HiltWorker con dipendenze
    // iniettate — senza questa factory WorkManager userebbe il costruttore no-arg di
    // default e fallirebbe a istanziarli.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // Scope per operazioni asincrone nell'Application
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Log.d(TAG, "🚀 QuickStore Application Starting...")

        createImageTransferNotificationChannel(this)

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
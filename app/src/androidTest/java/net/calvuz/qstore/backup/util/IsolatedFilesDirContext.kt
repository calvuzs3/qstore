package net.calvuz.qstore.backup.util

import android.content.Context
import android.content.ContextWrapper
import java.io.File

/**
 * Context di test che reindirizza filesDir/cacheDir a una directory temporanea isolata.
 *
 * Fondamentale per i test di backup: sia [net.calvuz.qstore.app.data.local.storage.ImageStorageManager]
 * che [net.calvuz.qstore.backup.data.repository.BackupRepositoryImpl] (clearAllImages/saveImageFile)
 * scrivono sotto context.filesDir/article_images — che su un device reale con l'app già
 * installata è la STESSA directory delle foto vere degli articoli dell'utente. Il Context
 * restituito da ApplicationProvider.getApplicationContext() in un test strumentato è quello
 * dell'app target, quindi senza questo wrapper i test cancellerebbero/sovrascriverebbero dati
 * reali sul device invece di operare su dati isolati.
 */
class IsolatedFilesDirContext(
    base: Context,
    private val isolatedFilesDir: File,
    private val isolatedCacheDir: File
) : ContextWrapper(base) {
    override fun getFilesDir(): File = isolatedFilesDir.apply { mkdirs() }
    override fun getCacheDir(): File = isolatedCacheDir.apply { mkdirs() }
}

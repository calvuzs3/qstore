package net.calvuz.qstore.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SimpleSQLiteQuery
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import net.calvuz.qstore.app.data.local.database.QuickStoreDatabase

/**
 * ContentProvider read-only che espone il catalogo articoli a QReport.
 *
 * URI:        content://net.calvuz.qstore.provider/articles
 * Permesso:   net.calvuz.qstore.permission.READ_ARTICLES (protectionLevel=signature)
 *
 * Supporta:
 *   - query con selection "uuid IN (?,?,?)"         → fetchByUuids
 *   - query con selection "name LIKE ? OR ..."      → ricerca type-ahead
 *   - notifyChange automatico via Room InvalidationTracker
 *
 * Non usa @AndroidEntryPoint per evitare problemi di inizializzazione
 * (ContentProvider può essere creato prima di Application.onCreate).
 * Il database viene acceduto in modo lazy tramite EntryPoint.
 */
class QStoreArticleProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun database(): QuickStoreDatabase
    }

    private val database: QuickStoreDatabase by lazy {
        val db = EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            ProviderEntryPoint::class.java
        ).database()
        // Registra l'observer una sola volta, qui, quando il database è pronto.
        db.invalidationTracker.addObserver(invalidationObserver)
        db
    }

    private val invalidationObserver = object : InvalidationTracker.Observer("articles") {
        override fun onInvalidated(tables: Set<String>) {
            context?.contentResolver?.notifyChange(ArticleContract.ARTICLES_URI, null)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != CODE_ARTICLES) return null

        val db = database.openHelper.readableDatabase

        val cols  = if (projection.isNullOrEmpty()) "*" else projection.joinToString(", ")
        val order = if (sortOrder.isNullOrBlank()) "name ASC" else sortOrder
        val sql   = buildString {
            append("SELECT $cols FROM articles")
            if (!selection.isNullOrBlank()) append(" WHERE $selection")
            append(" ORDER BY $order")
        }

        val args: Array<Any?>? = selectionArgs?.map { it as Any? }?.toTypedArray()
        val cursor = db.query(SimpleSQLiteQuery(sql, args))
        cursor.setNotificationUri(context!!.contentResolver, ArticleContract.ARTICLES_URI)
        return cursor
    }

    override fun getType(uri: Uri): String? =
        if (uriMatcher.match(uri) == CODE_ARTICLES)
            "vnd.android.cursor.dir/net.calvuz.qstore.articles"
        else null

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("QuickStore ContentProvider è read-only")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException("QuickStore ContentProvider è read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException("QuickStore ContentProvider è read-only")

    companion object {
        private const val CODE_ARTICLES = 1

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(ArticleContract.AUTHORITY, "articles", CODE_ARTICLES)
        }
    }
}

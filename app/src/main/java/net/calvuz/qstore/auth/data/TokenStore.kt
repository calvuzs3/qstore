package net.calvuz.qstore.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Storage cifrato per il token JWT (Keystore-backed via EncryptedSharedPreferences) —
 * non DataStore semplice come le altre impostazioni, perché qui è una credenziale, non
 * una preferenza.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private var prefs = try {
        createPrefs(context)
    } catch (e: Exception) {
        Timber.e(e, "Initial creation of EncryptedSharedPreferences failed, attempting recovery")
        context.deleteSharedPreferences(FILE_NAME)
        createPrefs(context)
    }

    private fun createPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getToken(): String? = try {
        prefs.getString(KEY_TOKEN, null)
    } catch (e: Exception) {
        Timber.e(e, "Error reading token from EncryptedSharedPreferences, clearing storage")
        handleError()
        null
    }

    // orgName non è un claim del JWT (solo il corpo della risposta di login/select-org lo
    // include) — va salvato a parte per sopravvivere a un riavvio dell'app senza un nuovo login.
    fun getOrgName(): String? = try {
        prefs.getString(KEY_ORG_NAME, null)
    } catch (e: Exception) {
        Timber.e(e, "Error reading org name from EncryptedSharedPreferences, clearing storage")
        handleError()
        null
    }

    fun save(token: String, orgName: String) {
        try {
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_ORG_NAME, orgName)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Error saving to EncryptedSharedPreferences, clearing storage")
            handleError()
        }
    }

    fun clear() {
        try {
            prefs.edit().remove(KEY_TOKEN).remove(KEY_ORG_NAME).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error clearing EncryptedSharedPreferences, attempting recovery")
            handleError()
        }
    }

    private fun handleError() {
        try {
            context.deleteSharedPreferences(FILE_NAME)
            prefs = createPrefs(context)
        } catch (e: Exception) {
            Timber.e(e, "Critical failure in TokenStore recovery")
        }
    }

    private companion object {
        const val FILE_NAME = "auth_token_store"
        const val KEY_TOKEN = "token"
        const val KEY_ORG_NAME = "org_name"
    }
}

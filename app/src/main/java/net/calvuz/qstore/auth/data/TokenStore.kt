package net.calvuz.qstore.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage cifrato per il token JWT (Keystore-backed via EncryptedSharedPreferences) —
 * non DataStore semplice come le altre impostazioni, perché qui è una credenziale, non
 * una preferenza.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_token_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    // orgName non è un claim del JWT (solo il corpo della risposta di login/select-org lo
    // include) — va salvato a parte per sopravvivere a un riavvio dell'app senza un nuovo login.
    fun getOrgName(): String? = prefs.getString(KEY_ORG_NAME, null)

    fun save(token: String, orgName: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ORG_NAME, orgName)
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_ORG_NAME).apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_ORG_NAME = "org_name"
    }
}

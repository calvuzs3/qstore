package net.calvuz.qstore.settings.domain.model

/**
 * Configurazione del server di sincronizzazione (quickstore-server).
 *
 * L'URL non è fisso: il server oggi gira su un IP privato di una VM, non un dominio
 * pubblico — l'utente lo configura da qui.
 */
data class ServerSettings(
    /** Es. "http://192.168.0.91:8081" — senza slash finale. */
    val baseUrl: String = ""
) {
    companion object {
        fun getDefault() = ServerSettings()
    }
}

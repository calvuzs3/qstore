package net.calvuz.qstore.settings.domain.model

/**
 * Configurazione del server di sincronizzazione (quickstore-server).
 *
 * Configurabile perché in fase di test si è puntato all'IP privato della VM
 * (es. "http://192.168.0.91:8081") — ora il server è raggiungibile pubblicamente in
 * HTTPS su "https://quickstore.calvuz.net", ma resta comunque un campo utente per non
 * dover ricompilare l'app se l'indirizzo cambia ancora.
 */
data class ServerSettings(
    /** Es. "https://quickstore.calvuz.net" — senza slash finale. */
    val baseUrl: String = ""
) {
    companion object {
        fun getDefault() = ServerSettings()
    }
}

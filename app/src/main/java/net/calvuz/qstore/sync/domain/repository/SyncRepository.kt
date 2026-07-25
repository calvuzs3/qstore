package net.calvuz.qstore.sync.domain.repository

import net.calvuz.qstore.sync.domain.model.BoundOrganization
import net.calvuz.qstore.sync.domain.model.PurgeSummary
import net.calvuz.qstore.sync.domain.model.SyncSummary

/**
 * Sincronizzazione manuale con quickstore-server: push delle righe locali modificate
 * dopo l'ultimo cursore, poi pull delle modifiche altrui. Richiede una sessione attiva
 * (vedi auth module) — l'app resta comunque utilizzabile offline senza mai chiamare questo.
 */
interface SyncRepository {
    suspend fun syncNow(): Result<SyncSummary>

    /**
     * Elimina fisicamente e per sempre articoli/immagini/categorie soft-eliminati da almeno
     * [retentionMillis]. Vedi PurgeDeletedDataUseCase per il vincolo di sicurezza sul cursore
     * di push (non purga mai una cancellazione non ancora propagata al server).
     */
    suspend fun purgeDeletedData(retentionMillis: Long): Result<PurgeSummary>

    /** Organizzazione a cui i dati locali sono legati — null se il device non ha mai sincronizzato. */
    suspend fun getBoundOrganization(): BoundOrganization?

    /**
     * Cancella per sempre TUTTI i dati locali (DB + JPEG) per liberare il device e permettergli
     * di legarsi a un'altra organizzazione. Vedi SwitchOrganizationUseCase — richiede conferma
     * esplicita dell'utente prima di essere chiamata, è irreversibile.
     */
    suspend fun switchOrganization(): Result<Unit>
}

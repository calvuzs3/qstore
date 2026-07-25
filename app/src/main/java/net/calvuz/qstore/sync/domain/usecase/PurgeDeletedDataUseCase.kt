package net.calvuz.qstore.sync.domain.usecase

import net.calvuz.qstore.sync.domain.model.PurgeSummary
import net.calvuz.qstore.sync.domain.repository.SyncRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Pulizia esplicita e a richiesta dell'utente dei dati soft-eliminati (articoli, immagini,
 * categorie) più vecchi della retention — mai automatica, mai in risposta a un sync. Vedi
 * SyncRepositoryImpl.purgeDeletedData per il vincolo di sicurezza sul cursore di push.
 */
class PurgeDeletedDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<PurgeSummary> =
        syncRepository.purgeDeletedData(RETENTION_MILLIS)

    companion object {
        // Abbastanza lungo da lasciare a un device poco attivo il tempo di sincronizzare la
        // cancellazione prima che la sua copia locale possa essere rimossa per sempre.
        private val RETENTION_MILLIS = TimeUnit.DAYS.toMillis(90)
    }
}

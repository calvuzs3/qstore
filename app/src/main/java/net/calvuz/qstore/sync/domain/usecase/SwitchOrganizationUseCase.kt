package net.calvuz.qstore.sync.domain.usecase

import net.calvuz.qstore.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Cancella per sempre tutti i dati locali (DB + JPEG) per liberare il device da
 * un'organizzazione e permettergli di legarsi a un'altra — vedi SyncRepositoryImpl per il
 * dettaglio del wipe. Va sempre confermato con un dialog lato UI prima di essere chiamato
 * (irreversibile), e va seguito da un logout esplicito lato ViewModel: qui non si tocca la
 * sessione, solo i dati sincronizzati.
 */
class SwitchOrganizationUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> = syncRepository.switchOrganization()
}

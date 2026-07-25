package net.calvuz.qstore.sync.domain.usecase

import net.calvuz.qstore.sync.domain.model.BoundOrganization
import net.calvuz.qstore.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Organizzazione a cui i dati locali sono legati — null se il device non ha mai sincronizzato
 * con successo. Usata da LoginViewModel subito dopo login/selezione org per avvisare l'utente
 * PRIMA che tenti un sync (che verrebbe comunque rifiutato da SyncRepositoryImpl.syncNow()).
 */
class GetBoundOrganizationUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): BoundOrganization? = syncRepository.getBoundOrganization()
}

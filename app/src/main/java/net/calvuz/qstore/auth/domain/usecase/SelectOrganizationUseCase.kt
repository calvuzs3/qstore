package net.calvuz.qstore.auth.domain.usecase

import net.calvuz.qstore.auth.domain.model.Session
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SelectOrganizationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pendingToken: String, orgId: String): Result<Session> {
        return authRepository.selectOrganization(pendingToken, orgId)
    }
}

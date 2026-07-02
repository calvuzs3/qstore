package net.calvuz.qstore.auth.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.auth.domain.model.Session
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Session?> = authRepository.observeSession()
}

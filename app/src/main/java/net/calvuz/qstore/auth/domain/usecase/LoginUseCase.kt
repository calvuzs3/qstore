package net.calvuz.qstore.auth.domain.usecase

import net.calvuz.qstore.auth.domain.model.LoginResult
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<LoginResult> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email obbligatoria"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password obbligatoria"))
        }
        return authRepository.login(email.trim(), password)
    }
}

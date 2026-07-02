package net.calvuz.qstore.auth.domain.model

/**
 * Esito di [net.calvuz.qstore.auth.domain.repository.AuthRepository.login] — rispecchia
 * il flusso in due passi del server: un utente con una sola organizzazione ottiene subito
 * una sessione, uno con più organizzazioni deve prima sceglierne una.
 */
sealed class LoginResult {
    data class Authenticated(val session: Session) : LoginResult()
    data class OrganizationSelectionRequired(
        val pendingToken: String,
        val organizations: List<OrganizationChoice>
    ) : LoginResult()
}

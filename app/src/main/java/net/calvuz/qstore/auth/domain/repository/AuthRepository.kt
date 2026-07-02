package net.calvuz.qstore.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.auth.domain.model.LoginResult
import net.calvuz.qstore.auth.domain.model.Session

/**
 * Repository per autenticazione e sessione verso quickstore-server.
 *
 * Login opzionale: QuickStore resta un'app offline-first completa senza account — questo
 * repository serve solo quando l'utente sceglie di attivare la sincronizzazione multi-device.
 */
interface AuthRepository {

    /** POST /auth/login. */
    suspend fun login(email: String, password: String): Result<LoginResult>

    /** POST /auth/select-org, quando login() ha richiesto una scelta tra più organizzazioni. */
    suspend fun selectOrganization(pendingToken: String, orgId: String): Result<Session>

    /** Sessione corrente, letta dal token salvato — null se non loggato. */
    fun observeSession(): Flow<Session?>

    /** Cancella il token salvato localmente (nessuna chiamata server: token stateless). */
    suspend fun logout()
}

package net.calvuz.qstore.sync.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import net.calvuz.qstore.sync.data.remote.dto.SyncPullResponse
import net.calvuz.qstore.sync.data.remote.dto.SyncPushRequest
import net.calvuz.qstore.sync.data.remote.dto.SyncPushResponse
import net.calvuz.qstore.sync.domain.model.SyncException
import java.io.IOException
import javax.inject.Inject

class SyncApi @Inject constructor(
    private val httpClient: HttpClient,
    private val serverSettingsRepository: ServerSettingsRepository,
    private val authRepository: AuthRepository
) {
    private suspend fun baseUrl(): String {
        val baseUrl = serverSettingsRepository.getSettings().first().baseUrl
        if (baseUrl.isBlank()) {
            throw SyncException("Indirizzo server non configurato (Impostazioni > Server di sincronizzazione)")
        }
        return baseUrl
    }

    private suspend fun token(): String {
        return authRepository.observeSession().first()?.token
            ?: throw SyncException("Devi accedere (Impostazioni > Account) prima di sincronizzare")
    }

    suspend fun push(payload: SyncPushRequest): SyncPushResponse {
        val response = request {
            httpClient.post("${baseUrl()}/sync/push") {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }
        return response.body()
    }

    suspend fun pull(since: Long): SyncPullResponse {
        val response = request {
            httpClient.get("${baseUrl()}/sync/pull") {
                bearerAuth(token())
                parameter("since", since)
            }
        }
        return response.body()
    }

    /** Controlla lo status PRIMA di leggere il body — stesso motivo di AuthApi. */
    private suspend fun request(block: suspend () -> HttpResponse): HttpResponse {
        val response = try {
            block()
        } catch (e: IOException) {
            throw SyncException("Impossibile contattare il server — verifica la connessione o l'indirizzo in Impostazioni")
        } catch (e: SyncException) {
            throw e
        } catch (e: Exception) {
            throw SyncException(e.message ?: "Errore imprevisto")
        }

        if (!response.status.isSuccess()) {
            throw SyncException(
                when (response.status) {
                    HttpStatusCode.Unauthorized -> "Sessione scaduta, accedi di nuovo"
                    HttpStatusCode.Forbidden -> "Permessi insufficienti per sincronizzare"
                    else -> "Richiesta non valida (${response.status.value})"
                }
            )
        }
        return response
    }
}

package net.calvuz.qstore.sync.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import net.calvuz.qstore.shared.dto.SyncPullResponse
import net.calvuz.qstore.shared.dto.SyncPushRequest
import net.calvuz.qstore.shared.dto.SyncPushResponse
import net.calvuz.qstore.sync.domain.model.SyncException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

private val log = Timber.tag("Sync")

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
        log.d("POST /sync/push deviceId=${payload.deviceId}")
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
        log.d("GET /sync/pull since=$since")
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
            log.e(e, "network error")
            throw SyncException("Impossibile contattare il server — verifica la connessione o l'indirizzo in Impostazioni")
        } catch (e: SyncException) {
            throw e
        } catch (e: Exception) {
            log.e(e, "unexpected error")
            throw SyncException(e.message ?: "Errore imprevisto")
        }

        log.d("response status=${response.status}")
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("<unreadable>")
            log.w("non-success response status=${response.status} body=$body")
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

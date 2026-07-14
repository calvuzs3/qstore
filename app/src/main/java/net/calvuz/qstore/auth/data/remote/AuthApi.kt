package net.calvuz.qstore.auth.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.calvuz.qstore.shared.dto.LoginOrgChoiceResponse
import net.calvuz.qstore.shared.dto.LoginRequest
import net.calvuz.qstore.shared.dto.LoginResponse
import net.calvuz.qstore.shared.dto.SelectOrgRequest
import net.calvuz.qstore.auth.domain.model.AuthException
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import java.io.IOException
import javax.inject.Inject

/** Risposta di /auth/login: le due forme non condivise da un discriminatore esplicito nel JSON. */
sealed class LoginApiResponse {
    data class FullToken(val dto: LoginResponse) : LoginApiResponse()
    data class OrgChoice(val dto: LoginOrgChoiceResponse) : LoginApiResponse()
}

class AuthApi @Inject constructor(
    private val httpClient: HttpClient,
    private val serverSettingsRepository: ServerSettingsRepository
) {
    private suspend fun baseUrl(): String {
        val baseUrl = serverSettingsRepository.getSettings().first().baseUrl
        require(baseUrl.isNotBlank()) { "Indirizzo server non configurato (Impostazioni > Server di sincronizzazione)" }
        return baseUrl
    }

    suspend fun login(email: String, password: String): LoginApiResponse {
        val response = request {
            httpClient.post("${baseUrl()}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
        }

        // Il server risponde con forme diverse per lo stesso endpoint (token pieno vs
        // pending+organizations), senza un campo discriminatore — si distingue guardando
        // quale chiave è presente nel JSON.
        val json = response.body<JsonObject>()
        return if (json.containsKey("pendingToken")) {
            LoginApiResponse.OrgChoice(Json.decodeFromJsonElement<LoginOrgChoiceResponse>(json))
        } else {
            LoginApiResponse.FullToken(Json.decodeFromJsonElement<LoginResponse>(json))
        }
    }

    suspend fun selectOrganization(pendingToken: String, orgId: String): LoginResponse {
        val response = request {
            httpClient.post("${baseUrl()}/auth/select-org") {
                contentType(ContentType.Application.Json)
                bearerAuth(pendingToken)
                setBody(SelectOrgRequest(orgId))
            }
        }
        return response.body()
    }

    /**
     * Esegue la richiesta e controlla lo status PRIMA di provare a leggere il body come
     * JSON — con `expectSuccess = false` (default di questo client) una risposta 401/403
     * non lancia da sola, arriva come risposta "normale" con un body testuale semplice
     * (es. "Invalid credentials"), che altrimenti fallirebbe la deserializzazione JSON con
     * un errore tecnico invece del messaggio pulito che vogliamo mostrare.
     */
    private suspend fun request(block: suspend () -> HttpResponse): HttpResponse {
        val response = try {
            block()
        } catch (e: IOException) {
            throw AuthException("Impossibile contattare il server — verifica la connessione o l'indirizzo in Impostazioni")
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException(e.message ?: "Errore imprevisto")
        }

        if (!response.status.isSuccess()) {
            throw AuthException(
                when (response.status) {
                    HttpStatusCode.Unauthorized -> "Email o password non corretti"
                    HttpStatusCode.Forbidden -> "Nessun accesso a questa organizzazione"
                    else -> "Richiesta non valida (${response.status.value})"
                }
            )
        }
        return response
    }
}

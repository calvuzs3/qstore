package net.calvuz.qstore.auth.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.calvuz.qstore.auth.data.remote.dto.LoginOrgChoiceResponseDto
import net.calvuz.qstore.auth.data.remote.dto.LoginRequestDto
import net.calvuz.qstore.auth.data.remote.dto.LoginResponseDto
import net.calvuz.qstore.auth.data.remote.dto.SelectOrgRequestDto
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import javax.inject.Inject

/** Risposta di /auth/login: le due forme non condivise da un discriminatore esplicito nel JSON. */
sealed class LoginApiResponse {
    data class FullToken(val dto: LoginResponseDto) : LoginApiResponse()
    data class OrgChoice(val dto: LoginOrgChoiceResponseDto) : LoginApiResponse()
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
        val response = httpClient.post("${baseUrl()}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email, password))
        }
        // Il server risponde con forme diverse per lo stesso endpoint (token pieno vs
        // pending+organizations), senza un campo discriminatore — si distingue guardando
        // quale chiave è presente nel JSON.
        val json = response.body<JsonObject>()
        return if (json.containsKey("pendingToken")) {
            LoginApiResponse.OrgChoice(Json.decodeFromJsonElement<LoginOrgChoiceResponseDto>(json))
        } else {
            LoginApiResponse.FullToken(Json.decodeFromJsonElement<LoginResponseDto>(json))
        }
    }

    suspend fun selectOrganization(pendingToken: String, orgId: String): LoginResponseDto {
        return httpClient.post("${baseUrl()}/auth/select-org") {
            contentType(ContentType.Application.Json)
            bearerAuth(pendingToken)
            setBody(SelectOrgRequestDto(orgId))
        }.body()
    }
}

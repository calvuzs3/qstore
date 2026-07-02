package net.calvuz.qstore.sync.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import net.calvuz.qstore.sync.domain.model.SyncException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

private val log = Timber.tag("Sync")

/**
 * Il file JPEG vero e proprio, separato dal payload di /sync/push|pull (che porta solo
 * imagePath/featuresData). La riga article_images deve già esistere via sync prima di
 * poter fare upload/download — vedi quickstore-server ImageRoutes.kt.
 */
class ImagesApi @Inject constructor(
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

    suspend fun uploadImage(id: String, bytes: ByteArray) {
        request {
            httpClient.post("${baseUrl()}/images/upload/$id") {
                bearerAuth(token())
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", bytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"$id.jpg\"")
                            })
                        }
                    )
                )
            }
        }
    }

    suspend fun downloadImage(id: String): ByteArray {
        val response = request {
            httpClient.get("${baseUrl()}/images/download/$id") {
                bearerAuth(token())
            }
        }
        return response.body()
    }

    /** Stesso pattern di status-check di SyncApi/AuthApi: mai leggere/fidarsi del body prima. */
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

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("<unreadable>")
            log.w("non-success response status=${response.status} body=$body")
            throw SyncException(
                when (response.status) {
                    HttpStatusCode.Unauthorized -> "Sessione scaduta, accedi di nuovo"
                    HttpStatusCode.Forbidden -> "Permessi insufficienti"
                    HttpStatusCode.NotFound -> "Immagine non trovata sul server"
                    HttpStatusCode.PayloadTooLarge -> "Immagine oltre il limite consentito (20MB)"
                    else -> "Richiesta non valida (${response.status.value})"
                }
            )
        }
        return response
    }
}

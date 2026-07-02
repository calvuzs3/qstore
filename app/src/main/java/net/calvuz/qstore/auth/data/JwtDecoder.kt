package net.calvuz.qstore.auth.data

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decodifica (senza verificare la firma — non serve, il client si fida del token che ha
 * appena ricevuto/salvato, la verifica è già avvenuta server-side) il payload di un JWT
 * per leggerne i claim, evitando una libreria JWT completa solo per questo.
 */
object JwtDecoder {

    fun decodePayload(token: String): JsonObject {
        val parts = token.split(".")
        require(parts.size == 3) { "Token JWT malformato" }
        val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        return Json.parseToJsonElement(payloadJson) as JsonObject
    }

    fun claim(payload: JsonObject, key: String): String? = payload[key]?.jsonPrimitive?.content

    fun intClaim(payload: JsonObject, key: String): Int? = payload[key]?.jsonPrimitive?.content?.toIntOrNull()

    fun longClaim(payload: JsonObject, key: String): Long? = payload[key]?.jsonPrimitive?.content?.toLongOrNull()

    /** Claim standard "exp" è in secondi Unix, non millisecondi. */
    fun isExpired(payload: JsonObject): Boolean {
        val expSeconds = longClaim(payload, "exp") ?: return true
        return expSeconds * 1000 <= System.currentTimeMillis()
    }
}

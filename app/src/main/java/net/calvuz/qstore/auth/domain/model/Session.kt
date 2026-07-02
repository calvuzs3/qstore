package net.calvuz.qstore.auth.domain.model

/**
 * Sessione autenticata: utente + organizzazione attiva. Il JWT è org-scoped — cambiare
 * organizzazione richiede un nuovo login/select-org, non un semplice switch locale.
 */
data class Session(
    val token: String,
    val userId: String,
    val orgId: String,
    val orgName: String,
    val roleLevel: Int,
    val roleCode: String
)

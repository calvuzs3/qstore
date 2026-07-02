package net.calvuz.qstore.auth.domain.model

/**
 * Una delle organizzazioni tra cui l'utente può scegliere dopo il login, quando ne ha
 * più di una (flusso in due passi: login -> select-org).
 */
data class OrganizationChoice(
    val id: String,
    val name: String,
    val roleLevel: Int,
    val roleCode: String
)

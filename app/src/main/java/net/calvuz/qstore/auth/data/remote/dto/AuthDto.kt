package net.calvuz.qstore.auth.data.remote.dto

import kotlinx.serialization.Serializable

// Stessa shape esatta dei DTO server-side (quickstore-server AuthDto.kt) — nessun
// @SerialName necessario, i nomi dei campi kotlinx.serialization coincidono già.

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class OrganizationSummaryDto(
    val id: String,
    val name: String,
    val roleLevel: Int,
    val roleCode: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val orgId: String,
    val orgName: String,
    val roleLevel: Int,
    val roleCode: String
)

@Serializable
data class LoginOrgChoiceResponseDto(
    val pendingToken: String,
    val organizations: List<OrganizationSummaryDto>
)

@Serializable
data class SelectOrgRequestDto(val orgId: String)

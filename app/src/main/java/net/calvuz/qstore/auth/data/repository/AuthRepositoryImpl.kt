package net.calvuz.qstore.auth.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.calvuz.qstore.auth.data.JwtDecoder
import net.calvuz.qstore.auth.data.TokenStore
import net.calvuz.qstore.auth.data.remote.AuthApi
import net.calvuz.qstore.auth.data.remote.LoginApiResponse
import net.calvuz.qstore.shared.dto.LoginResponse
import net.calvuz.qstore.auth.domain.model.LoginResult
import net.calvuz.qstore.auth.domain.model.OrganizationChoice
import net.calvuz.qstore.auth.domain.model.Session
import net.calvuz.qstore.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) : AuthRepository {

    private val _session = MutableStateFlow(restoreSession())
    private val session: StateFlow<Session?> = _session.asStateFlow()

    override suspend fun login(email: String, password: String): Result<LoginResult> {
        return try {
            when (val response = authApi.login(email, password)) {
                is LoginApiResponse.FullToken -> {
                    val newSession = persistAndBuildSession(response.dto)
                    Result.success(LoginResult.Authenticated(newSession))
                }
                is LoginApiResponse.OrgChoice -> {
                    val organizations = response.dto.organizations.map {
                        OrganizationChoice(it.id, it.name, it.roleLevel, it.roleCode)
                    }
                    Result.success(
                        LoginResult.OrganizationSelectionRequired(response.dto.pendingToken, organizations)
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectOrganization(pendingToken: String, orgId: String): Result<Session> {
        return try {
            val dto = authApi.selectOrganization(pendingToken, orgId)
            Result.success(persistAndBuildSession(dto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSession(): Flow<Session?> = session

    override suspend fun logout() {
        tokenStore.clear()
        _session.value = null
    }

    private fun persistAndBuildSession(dto: LoginResponse): Session {
        tokenStore.save(dto.token, dto.orgName)
        val payload = JwtDecoder.decodePayload(dto.token)
        val userId = JwtDecoder.claim(payload, "sub")
            ?: error("Token di risposta senza claim 'sub'")
        val newSession = Session(
            token = dto.token,
            userId = userId,
            orgId = dto.orgId,
            orgName = dto.orgName,
            roleLevel = dto.roleLevel,
            roleCode = dto.roleCode
        )
        _session.value = newSession
        return newSession
    }

    private fun restoreSession(): Session? {
        val token = tokenStore.getToken() ?: return null
        val orgName = tokenStore.getOrgName() ?: return null
        return try {
            val payload = JwtDecoder.decodePayload(token)
            if (JwtDecoder.isExpired(payload)) {
                tokenStore.clear()
                return null
            }
            Session(
                token = token,
                userId = JwtDecoder.claim(payload, "sub") ?: return null,
                orgId = JwtDecoder.claim(payload, "orgId") ?: return null,
                orgName = orgName,
                roleLevel = JwtDecoder.intClaim(payload, "roleLevel") ?: return null,
                roleCode = JwtDecoder.claim(payload, "roleCode") ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }
}

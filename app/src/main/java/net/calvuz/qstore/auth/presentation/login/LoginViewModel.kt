package net.calvuz.qstore.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.calvuz.qstore.auth.domain.model.LoginResult
import net.calvuz.qstore.auth.domain.model.OrganizationChoice
import net.calvuz.qstore.auth.domain.model.Session
import net.calvuz.qstore.auth.domain.usecase.LoginUseCase
import net.calvuz.qstore.auth.domain.usecase.LogoutUseCase
import net.calvuz.qstore.auth.domain.usecase.ObserveSessionUseCase
import net.calvuz.qstore.auth.domain.usecase.SelectOrganizationUseCase
import net.calvuz.qstore.sync.domain.usecase.SyncNowUseCase
import javax.inject.Inject

sealed class LoginUiState {
    data class LoginForm(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
    ) : LoginUiState()

    data class OrgSelection(
        val pendingToken: String,
        val organizations: List<OrganizationChoice>,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : LoginUiState()

    /** Sessione già presente all'apertura dello schermo, o appena ottenuta con un login fresco. */
    data class AlreadyLoggedIn(
        val session: Session,
        val isLoggingOut: Boolean = false,
        val isSyncing: Boolean = false,
        val syncMessage: String? = null
    ) : LoginUiState()

    /** Transiente: solo per far scattare onLoggedIn() dopo un login appena riuscito. */
    data object JustLoggedIn : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val selectOrganizationUseCase: SelectOrganizationUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val syncNowUseCase: SyncNowUseCase,
    observeSessionUseCase: ObserveSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.LoginForm())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().first()?.let { session ->
                _uiState.value = LoginUiState.AlreadyLoggedIn(session)
            }
        }
    }

    fun updateEmail(email: String) {
        (_uiState.value as? LoginUiState.LoginForm)?.let {
            _uiState.value = it.copy(email = email, error = null)
        }
    }

    fun updatePassword(password: String) {
        (_uiState.value as? LoginUiState.LoginForm)?.let {
            _uiState.value = it.copy(password = password, error = null)
        }
    }

    fun submitLogin() {
        val form = _uiState.value as? LoginUiState.LoginForm ?: return
        _uiState.value = form.copy(isLoading = true, error = null)

        viewModelScope.launch {
            loginUseCase(form.email, form.password)
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is LoginResult.Authenticated -> LoginUiState.JustLoggedIn
                        is LoginResult.OrganizationSelectionRequired -> LoginUiState.OrgSelection(
                            pendingToken = result.pendingToken,
                            organizations = result.organizations
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.value = form.copy(isLoading = false, error = throwable.message ?: "Errore di login")
                }
        }
    }

    fun selectOrganization(orgId: String) {
        val state = _uiState.value as? LoginUiState.OrgSelection ?: return
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            selectOrganizationUseCase(state.pendingToken, orgId)
                .onSuccess {
                    _uiState.value = LoginUiState.JustLoggedIn
                }
                .onFailure { throwable ->
                    _uiState.value = state.copy(isLoading = false, error = throwable.message ?: "Errore di selezione organizzazione")
                }
        }
    }

    fun logout() {
        val state = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return
        _uiState.value = state.copy(isLoggingOut = true)
        viewModelScope.launch {
            logoutUseCase()
            _uiState.value = LoginUiState.LoginForm()
        }
    }

    fun syncNow() {
        val state = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return
        _uiState.value = state.copy(isSyncing = true, syncMessage = null)

        viewModelScope.launch {
            syncNowUseCase()
                .onSuccess { summary ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onSuccess
                    _uiState.value = current.copy(
                        isSyncing = false,
                        syncMessage = "Sincronizzato: ${summary.pushedCount} inviati, ${summary.pulledCount} ricevuti" +
                            if (summary.rejectedCount > 0) ", ${summary.rejectedCount} rifiutati" else ""
                    )
                }
                .onFailure { throwable ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onFailure
                    _uiState.value = current.copy(isSyncing = false, syncMessage = throwable.message ?: "Errore di sincronizzazione")
                }
        }
    }
}

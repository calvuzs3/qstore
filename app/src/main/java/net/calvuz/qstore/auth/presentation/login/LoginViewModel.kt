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
import net.calvuz.qstore.app.domain.usecase.movement.ReconcileInventoryMovementsUseCase
import net.calvuz.qstore.sync.domain.usecase.ObserveAllowMeteredNetworkUseCase
import net.calvuz.qstore.sync.domain.usecase.SetAllowMeteredNetworkUseCase
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

    /**
     * Sessione già presente all'apertura dello schermo, o appena ottenuta con un login
     * fresco — in entrambi i casi la UI mostra subito i pulsanti "Sincronizza ora"/
     * "Disconnetti", senza dover navigare via e tornare indietro. [justLoggedIn] fa
     * comparire una volta sola lo snackbar di conferma, poi va azzerato.
     */
    data class AlreadyLoggedIn(
        val session: Session,
        val isLoggingOut: Boolean = false,
        val isSyncing: Boolean = false,
        val syncMessage: String? = null,
        val justLoggedIn: Boolean = false,
        val allowMeteredNetwork: Boolean = false,
        val isReconciling: Boolean = false,
        val reconcileMessage: String? = null
    ) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val selectOrganizationUseCase: SelectOrganizationUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val syncNowUseCase: SyncNowUseCase,
    private val reconcileInventoryMovementsUseCase: ReconcileInventoryMovementsUseCase,
    private val observeAllowMeteredNetworkUseCase: ObserveAllowMeteredNetworkUseCase,
    private val setAllowMeteredNetworkUseCase: SetAllowMeteredNetworkUseCase,
    observeSessionUseCase: ObserveSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.LoginForm())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().first()?.let { session ->
                _uiState.value = LoginUiState.AlreadyLoggedIn(
                    session = session,
                    allowMeteredNetwork = observeAllowMeteredNetworkUseCase().first()
                )
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
                        is LoginResult.Authenticated -> LoginUiState.AlreadyLoggedIn(
                            session = result.session,
                            justLoggedIn = true,
                            allowMeteredNetwork = observeAllowMeteredNetworkUseCase().first()
                        )
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
                .onSuccess { session ->
                    _uiState.value = LoginUiState.AlreadyLoggedIn(
                        session = session,
                        justLoggedIn = true,
                        allowMeteredNetwork = observeAllowMeteredNetworkUseCase().first()
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = state.copy(isLoading = false, error = throwable.message ?: "Errore di selezione organizzazione")
                }
        }
    }

    fun setAllowMeteredNetwork(allow: Boolean) {
        val state = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return
        _uiState.value = state.copy(allowMeteredNetwork = allow)
        viewModelScope.launch { setAllowMeteredNetworkUseCase(allow) }
    }

    fun clearJustLoggedIn() {
        (_uiState.value as? LoginUiState.AlreadyLoggedIn)?.let {
            _uiState.value = it.copy(justLoggedIn = false)
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
                            (if (summary.rejectedCount > 0) ", ${summary.rejectedCount} rifiutati" else "") +
                            (if (summary.failedMovements > 0) ", ${summary.failedMovements} movimenti falliti" else "")
                    )
                }
                .onFailure { throwable ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onFailure
                    _uiState.value = current.copy(isSyncing = false, syncMessage = throwable.message ?: "Errore di sincronizzazione")
                }
        }
    }

    /**
     * Solo debug (vedi LoginScreen): ripara gli articoli creati prima del fix di
     * AddArticleUseCase, la cui giacenza iniziale era scritta direttamente in inventory
     * senza generare un movimento — quindi mai propagata dal sync. Da lanciare una volta
     * sola, poi tappare "Sincronizza ora" per pushare le correzioni generate.
     */
    fun reconcileInventoryMovements() {
        val state = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return
        _uiState.value = state.copy(isReconciling = true, reconcileMessage = null)

        viewModelScope.launch {
            reconcileInventoryMovementsUseCase()
                .onSuccess { fixedCount ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onSuccess
                    _uiState.value = current.copy(
                        isReconciling = false,
                        reconcileMessage = if (fixedCount > 0) {
                            "Creati $fixedCount movimenti correttivi — ora tocca Sincronizza ora per pusharli"
                        } else {
                            "Nessuna correzione necessaria"
                        }
                    )
                }
                .onFailure { throwable ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onFailure
                    _uiState.value = current.copy(isReconciling = false, reconcileMessage = throwable.message ?: "Errore riconciliazione")
                }
        }
    }
}

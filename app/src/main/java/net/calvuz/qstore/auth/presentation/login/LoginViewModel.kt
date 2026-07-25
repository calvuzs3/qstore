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
import net.calvuz.qstore.sync.domain.model.BoundOrganization
import net.calvuz.qstore.sync.domain.usecase.GetBoundOrganizationUseCase
import net.calvuz.qstore.sync.domain.usecase.ObserveAllowMeteredNetworkUseCase
import net.calvuz.qstore.sync.domain.usecase.PurgeDeletedDataUseCase
import net.calvuz.qstore.sync.domain.usecase.SetAllowMeteredNetworkUseCase
import net.calvuz.qstore.sync.domain.usecase.SwitchOrganizationUseCase
import net.calvuz.qstore.sync.domain.usecase.SyncNowUseCase
import javax.inject.Inject

sealed class LoginUiState {
    data class LoginForm(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        // Messaggio informativo one-shot, es. dopo un cambio organizzazione riuscito — non è
        // un errore, riusa lo stesso canale snackbar di `error` lato UI ma semanticamente diverso.
        val info: String? = null
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
        val reconcileMessage: String? = null,
        val isPurging: Boolean = false,
        val purgeMessage: String? = null,
        // Non-null quando questo device ha già dati sincronizzati con un'ALTRA organizzazione
        // rispetto a quella appena autenticata — vedi GetBoundOrganizationUseCase. Finché non
        // nullo, "Sincronizza ora" va disabilitato: SyncRepositoryImpl.syncNow() lo rifiuterebbe
        // comunque, ma bloccarlo qui evita all'utente di scoprirlo solo dopo aver premuto sync.
        val orgMismatch: BoundOrganization? = null,
        val isSwitchingOrganization: Boolean = false,
        val switchMessage: String? = null
    ) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val selectOrganizationUseCase: SelectOrganizationUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val syncNowUseCase: SyncNowUseCase,
    private val purgeDeletedDataUseCase: PurgeDeletedDataUseCase,
    private val getBoundOrganizationUseCase: GetBoundOrganizationUseCase,
    private val switchOrganizationUseCase: SwitchOrganizationUseCase,
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
                _uiState.value = buildAlreadyLoggedInState(session, justLoggedIn = false)
            }
        }
    }

    /**
     * Costruisce lo stato AlreadyLoggedIn includendo il controllo org — condiviso da init{},
     * login fresco e selezione organizzazione così il controllo non si scorda in un punto.
     */
    private suspend fun buildAlreadyLoggedInState(session: Session, justLoggedIn: Boolean): LoginUiState.AlreadyLoggedIn {
        val bound = getBoundOrganizationUseCase()
        return LoginUiState.AlreadyLoggedIn(
            session = session,
            justLoggedIn = justLoggedIn,
            allowMeteredNetwork = observeAllowMeteredNetworkUseCase().first(),
            orgMismatch = bound?.takeIf { it.orgId != session.orgId }
        )
    }

    fun updateEmail(email: String) {
        (_uiState.value as? LoginUiState.LoginForm)?.let {
            _uiState.value = it.copy(email = email, error = null, info = null)
        }
    }

    fun updatePassword(password: String) {
        (_uiState.value as? LoginUiState.LoginForm)?.let {
            _uiState.value = it.copy(password = password, error = null, info = null)
        }
    }

    fun submitLogin() {
        val form = _uiState.value as? LoginUiState.LoginForm ?: return
        _uiState.value = form.copy(isLoading = true, error = null)

        viewModelScope.launch {
            loginUseCase(form.email, form.password)
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is LoginResult.Authenticated ->
                            buildAlreadyLoggedInState(result.session, justLoggedIn = true)
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
                    _uiState.value = buildAlreadyLoggedInState(session, justLoggedIn = true)
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
     * Pulizia esplicita e a richiesta: elimina per sempre articoli/foto/categorie già
     * soft-eliminati da tempo e già sincronizzati (vedi PurgeDeletedDataUseCase). Va sempre
     * confermata da un dialog lato UI prima di essere chiamata — è irreversibile.
     */
    fun purgeDeletedData() {
        val state = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return
        _uiState.value = state.copy(isPurging = true, purgeMessage = null)

        viewModelScope.launch {
            purgeDeletedDataUseCase()
                .onSuccess { summary ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onSuccess
                    _uiState.value = current.copy(
                        isPurging = false,
                        purgeMessage = if (summary.total > 0) {
                            "Eliminati definitivamente: ${summary.articlesPurged} articoli, " +
                                "${summary.imagesPurged} foto, ${summary.categoriesPurged} categorie"
                        } else {
                            "Nessun dato cancellato abbastanza vecchio da rimuovere"
                        }
                    )
                }
                .onFailure { throwable ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onFailure
                    _uiState.value = current.copy(isPurging = false, purgeMessage = throwable.message ?: "Errore durante la pulizia")
                }
        }
    }

    /**
     * Cambio organizzazione esplicito e a richiesta: cancella per sempre tutti i dati locali
     * (vedi SwitchOrganizationUseCase — un backup di sicurezza automatico parte da solo prima
     * del wipe) e disconnette, riportando l'utente al form di login per accedere con
     * l'organizzazione diversa. Va sempre confermata da un dialog lato UI — è irreversibile.
     */
    fun switchOrganization() {
        val state = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return
        _uiState.value = state.copy(isSwitchingOrganization = true, switchMessage = null)

        viewModelScope.launch {
            switchOrganizationUseCase()
                .onSuccess {
                    logoutUseCase()
                    _uiState.value = LoginUiState.LoginForm(
                        info = "Dati locali cancellati. Accedi con la nuova organizzazione."
                    )
                }
                .onFailure { throwable ->
                    val current = _uiState.value as? LoginUiState.AlreadyLoggedIn ?: return@onFailure
                    _uiState.value = current.copy(
                        isSwitchingOrganization = false,
                        switchMessage = throwable.message ?: "Errore durante il cambio organizzazione"
                    )
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

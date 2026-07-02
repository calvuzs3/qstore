package net.calvuz.qstore.auth.domain.model

/** Errore di auth con un messaggio già pronto per l'utente (mai un'eccezione tecnica grezza). */
class AuthException(message: String) : Exception(message)

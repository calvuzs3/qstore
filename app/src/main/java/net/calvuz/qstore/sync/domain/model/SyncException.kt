package net.calvuz.qstore.sync.domain.model

/** Errore di sync con un messaggio già pronto per l'utente (mai un'eccezione tecnica grezza). */
class SyncException(message: String) : Exception(message)

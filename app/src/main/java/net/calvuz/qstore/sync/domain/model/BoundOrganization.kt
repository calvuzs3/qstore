package net.calvuz.qstore.sync.domain.model

/**
 * Organizzazione a cui i dati sincronizzati di questo device sono attualmente legati (vedi
 * SyncLocalStore/SyncRepositoryImpl). Un device resta legato alla prima organizzazione con cui
 * fa un sync riuscito, finché non viene esplicitamente cambiato (SwitchOrganizationUseCase).
 */
data class BoundOrganization(val orgId: String, val orgName: String)

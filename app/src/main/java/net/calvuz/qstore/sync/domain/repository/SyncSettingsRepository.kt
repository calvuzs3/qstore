package net.calvuz.qstore.sync.domain.repository

import kotlinx.coroutines.flow.Flow

/** Preferenze di sync esposte alla presentation layer (implementata da SyncLocalStore). */
interface SyncSettingsRepository {
    /** Default false (solo wifi): il trasferimento foto (ImageTransferWorker) può essere pesante. */
    fun observeAllowMeteredNetworkForImages(): Flow<Boolean>
    suspend fun setAllowMeteredNetworkForImages(allow: Boolean)
}

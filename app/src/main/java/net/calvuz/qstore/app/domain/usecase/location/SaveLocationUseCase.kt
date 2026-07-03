package net.calvuz.qstore.app.domain.usecase.location

import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.repository.LocationRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per creare/aggiornare un magazzino/ubicazione
 */
class SaveLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    /**
     * Crea un nuovo magazzino
     */
    suspend fun create(
        name: String,
        notes: String = ""
    ): Result<Location> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Il nome è obbligatorio"))
        }

        val now = System.currentTimeMillis()
        val location = Location(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )

        return locationRepository.insert(location).map { location }
    }

    /**
     * Aggiorna un magazzino esistente
     */
    suspend fun update(
        uuid: String,
        name: String,
        notes: String
    ): Result<Location> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Il nome è obbligatorio"))
        }

        val existing = locationRepository.getByUuid(uuid).getOrElse { return Result.failure(it) }
            ?: return Result.failure(IllegalArgumentException("Magazzino non trovato"))

        val updated = existing.copy(
            name = name.trim(),
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )

        return locationRepository.update(updated).map { updated }
    }
}

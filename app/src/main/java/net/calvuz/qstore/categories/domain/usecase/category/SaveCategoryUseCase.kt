package net.calvuz.qstore.categories.domain.usecase.category

import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import java.util.UUID
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val repository: ArticleCategoryRepository
) {
    /**
     * Crea una nuova categoria
     */
    suspend fun create(
        name: String,
        description: String = "",
        notes: String = ""
    ): Result<ArticleCategory> {
        // Validazione
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Il nome è obbligatorio"))
        }

        val now = System.currentTimeMillis()
        val category = ArticleCategory(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )

        return repository.insert(category).map { category }
    }

    /**
     * Aggiorna una categoria esistente
     */
    suspend fun update(
        uuid: String,
        name: String,
        description: String,
        notes: String
    ): Result<ArticleCategory> {
        // Validazione
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Il nome è obbligatorio"))
        }

        val existing = repository.getByUuid(uuid)
            ?: return Result.failure(IllegalArgumentException("Categoria non trovata"))

        val updated = existing.copy(
            name = name.trim(),
            description = description.trim(),
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )

        return repository.update(updated).map { updated }
    }
}

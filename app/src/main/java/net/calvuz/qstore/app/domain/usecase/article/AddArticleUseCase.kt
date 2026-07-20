package net.calvuz.qstore.app.domain.usecase.article

import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.usecase.movement.AddMovementUseCase
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per aggiungere un nuovo articolo con inventario iniziale
 */
class AddArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val addMovementUseCase: AddMovementUseCase
) {
    /**
     * Aggiunge un nuovo articolo al magazzino
     *
     * @param name Nome articolo (obbligatorio)
     * @param description Descrizione (opzionale)
     * @param unitOfMeasure Unità di misura (es: "PZ", "KG", "L")
     * @param category Categoria (opzionale)
     * @param initialQuantity Quantità iniziale in magazzino
     * @return Result con Article creato, errore altrimenti
     */
    suspend operator fun invoke(
        name: String,
        description: String,
        recorderLevel: Double,
        codeOEM: String,
        codeERP: String,
        codeBM: String,
        notes: String,
        unitOfMeasure: String,
        categoryId: String,
        initialQuantity: Double = 0.0
    ): Result<Article> {
        // Validazione input
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Article name cannot be blank"))
        }

        if (unitOfMeasure.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit of measure cannot be blank"))
        }

        if (initialQuantity < 0) {
            return Result.failure(IllegalArgumentException("Initial quantity cannot be negative"))
        }

        // Crea articolo
        val currentTime = System.currentTimeMillis()
        val article = Article(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            reorderLevel = recorderLevel,
            codeOEM = codeOEM.trim(),
            codeERP = codeERP.trim(),
            codeBM = codeBM.trim(),
            notes = notes.trim(),
            unitOfMeasure = unitOfMeasure.trim().uppercase(),
            categoryId = categoryId.trim(),
            createdAt = currentTime,
            updatedAt = currentTime
        )

        // Salva l'anagrafica; la giacenza iniziale (se presente) è un movimento IN, non una
        // scrittura diretta su inventory — altrimenti non genera storico e resta invisibile
        // al sync, che propaga solo movements (vedi memoria bug-initial-quantity-no-movement).
        return articleRepository.insertArticle(article).fold(
            onSuccess = {
                if (initialQuantity > 0) {
                    addMovementUseCase.addIncoming(
                        articleUuid = article.uuid,
                        quantity = initialQuantity,
                        note = "Giacenza iniziale"
                    ).fold(
                        onSuccess = { Result.success(article) },
                        onFailure = { Result.failure(it) }
                    )
                } else {
                    Result.success(article)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}
package net.calvuz.qstore.data.mapper

import net.calvuz.qstore.data.local.entity.ArticleImageEntity
import net.calvuz.qstore.domain.model.ArticleImage
import javax.inject.Inject

/**
 * Mapper per convertire tra ArticleImageEntity (data layer) e ArticleImage (domain layer)
 */
class ArticleImageMapper @Inject constructor() {
    fun toDomain(entity: ArticleImageEntity): ArticleImage {
        return ArticleImage(
            id = entity.id,
            articleUuid = entity.articleUuid,
            imagePath = entity.imagePath,
            featuresData = entity.featuresData,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(domain: ArticleImage): ArticleImageEntity {
        return ArticleImageEntity(
            id = domain.id,
            articleUuid = domain.articleUuid,
            imagePath = domain.imagePath,
            featuresData = domain.featuresData,
            createdAt = domain.createdAt
        )
    }

    /**
     * Converte una lista di Entity in lista di Domain Models
     */
    fun toDomainList(entities: List<ArticleImageEntity>): List<ArticleImage> {
        return entities.map { toDomain(it) }
    }
}
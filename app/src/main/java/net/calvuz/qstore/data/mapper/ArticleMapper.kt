package net.calvuz.qstore.data.mapper

import net.calvuz.qstore.data.local.entity.ArticleEntity
import net.calvuz.qstore.domain.model.Article
import javax.inject.Inject

/**
 * Mapper per convertire tra ArticleEntity (data layer) e Article (domain layer)
 */
class ArticleMapper @Inject constructor() {

    /**
     * Converte da Entity a Domain Model
     */
    fun toDomain(entity: ArticleEntity): Article {
        return Article(
            uuid = entity.uuid,
            name = entity.name,
            description = entity.description,
            categoryId = entity.categoryId,
            unitOfMeasure = entity.unitOfMeasure,
            reorderLevel = entity.reorderLevel,
            codeOEM = entity.codeOEM,
            codeERP = entity.codeERP,
            codeBM = entity.codeBM,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converte da Domain Model a Entity
     */
    fun toEntity(domain: Article): ArticleEntity {
        return ArticleEntity(
            uuid = domain.uuid,
            name = domain.name,
            description = domain.description,
            categoryId = domain.categoryId,
            unitOfMeasure = domain.unitOfMeasure,
            reorderLevel = domain.reorderLevel,
            codeOEM = domain.codeOEM,
            codeERP = domain.codeERP,
            codeBM = domain.codeBM,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Lista Entity → Lista Domain
     */
    fun toDomainList(entities: List<ArticleEntity>): List<Article> {
        return entities.map { toDomain(it) }
    }

    /**
     * Lista Domain → Lista Entity
     */
    fun toEntityList(domains: List<Article>): List<ArticleEntity> {
        return domains.map { toEntity(it) }
    }
}
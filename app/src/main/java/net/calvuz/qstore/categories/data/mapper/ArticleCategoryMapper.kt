package net.calvuz.qstore.categories.data.mapper

import net.calvuz.qstore.app.data.local.entity.ArticleCategoryEntity
import net.calvuz.qstore.categories.domain.model.ArticleCategory

fun ArticleCategoryEntity.toDomain(): ArticleCategory {
    return ArticleCategory(
        uuid = uuid,
        name = name,
        description = description,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ArticleCategory.toEntity(): ArticleCategoryEntity {
    return ArticleCategoryEntity(
        uuid = uuid,
        name = name,
        description = description,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun List<ArticleCategoryEntity>.toDomainList(): List<ArticleCategory> {
    return map { it.toDomain() }
}

fun List<ArticleCategory>.toEntityList(): List<ArticleCategoryEntity> {
    return map { it.toEntity() }
}

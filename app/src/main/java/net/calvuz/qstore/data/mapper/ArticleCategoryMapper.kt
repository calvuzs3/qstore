package net.calvuz.qstore.data.mapper

import net.calvuz.qstore.data.local.entity.ArticleCategoryEntity
import net.calvuz.qstore.domain.model.ArticleCategory

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

fun List<ArticleCategoryEntity>.toDomainList(): List<ArticleCategory> = map { it.toDomain() }
fun List<ArticleCategory>.toEntityList(): List<ArticleCategoryEntity> = map { it.toEntity() }
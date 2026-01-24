package net.calvuz.qstore.app.domain.model

data class ArticleCategory(
    val uuid: String,
    val name: String,
    val description: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)
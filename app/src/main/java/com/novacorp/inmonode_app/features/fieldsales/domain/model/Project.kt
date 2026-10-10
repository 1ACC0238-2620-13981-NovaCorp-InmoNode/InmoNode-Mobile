package com.novacorp.inmonode_app.features.fieldsales.domain.model

data class Project(
    val id: Long,
    val name: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val coverImageUrl: String?,
    val financingRules: FinancingRules,
    val lots: List<Lot>,
)

data class Portfolio(val projects: List<Project>, val downloadedAt: String?, val etag: String?)

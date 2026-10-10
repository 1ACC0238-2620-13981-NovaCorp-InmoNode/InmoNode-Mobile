package com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote

import java.math.BigDecimal

data class FieldPortfolioDto(val projects: List<PortfolioProjectDto>)
data class PortfolioProjectDto(val id: Long, val name: String, val location: String,
    val latitude: Double?, val longitude: Double?, val coverImageUrl: String?,
    val financingRules: FinancingRulesDto, val lots: LotFeatureCollectionDto)
data class FinancingRulesDto(val minDownPaymentPercentage: BigDecimal,
    val annualInterestRate: BigDecimal, val maxTermMonths: Int, val lateFeeRate: BigDecimal)

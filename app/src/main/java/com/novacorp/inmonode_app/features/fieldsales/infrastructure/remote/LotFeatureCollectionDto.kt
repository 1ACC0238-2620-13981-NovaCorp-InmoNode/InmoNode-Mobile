package com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote

import java.math.BigDecimal

data class LotFeatureCollectionDto(val type: String, val features: List<LotFeatureDto>)
data class LotFeatureDto(val type: String, val id: Long, val geometry: LotGeometryDto, val properties: LotPropertiesDto)
data class LotGeometryDto(val type: String, val coordinates: List<List<List<Double>>>)
data class LotPropertiesDto(val code: String, val area: BigDecimal, val front: BigDecimal,
    val depth: BigDecimal, val price: BigDecimal, val currency: String, val status: String)

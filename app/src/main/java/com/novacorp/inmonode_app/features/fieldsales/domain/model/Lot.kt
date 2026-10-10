package com.novacorp.inmonode_app.features.fieldsales.domain.model

import java.math.BigDecimal

data class GeoPoint(val longitude: Double, val latitude: Double)

data class Lot(
    val id: Long,
    val projectId: Long,
    val code: String,
    val dimensions: LotDimensions,
    val polygon: List<List<GeoPoint>>,
    val price: BigDecimal,
    val currency: String,
    val status: LotStatus,
    val pendingReservationId: String? = null,
) {
    fun isAvailable(): Boolean = status == LotStatus.AVAILABLE && pendingReservationId == null
}

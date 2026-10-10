package com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote

import java.math.BigDecimal

data class FieldSyncRequestDto(val prospects: List<ProspectRecordDto>, val reservations: List<ReservationRecordDto>)
data class ProspectRecordDto(val id: String, val document: String, val fullName: String,
    val phone: String, val registeredAt: String)
data class ReservationRecordDto(val id: String, val lotId: Long, val prospectId: String,
    val initialAmount: BigDecimal, val reservedAt: String)

package com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote

import java.math.BigDecimal

data class FieldSyncRequestDto(val prospects: List<ProspectRecordDto>, val reservations: List<ReservationRecordDto>)
data class ProspectRecordDto(val id: String, val document: String, val fullName: String,
    val phone: String, val registeredAt: String)
/**
 * The negotiation terms (US-58) are optional and Gson omits them when null. Until the backend reads them (T52),
 * Spring ignores these unknown fields, so sending them early is safe.
 */
data class ReservationRecordDto(val id: String, val lotId: Long, val prospectId: String,
    val initialAmount: BigDecimal, val reservedAt: String, val discountAmount: BigDecimal? = null,
    val bonus: String? = null, val negotiationNotes: String? = null)

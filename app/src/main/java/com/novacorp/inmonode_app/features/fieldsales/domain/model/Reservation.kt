package com.novacorp.inmonode_app.features.fieldsales.domain.model

import java.math.BigDecimal

data class Reservation(
    val id: String,
    val lotId: Long,
    val prospectId: String,
    val initialAmount: BigDecimal,
    val reservedAt: String,
    val status: ReservationStatus = ReservationStatus.PENDING_SYNC,
    val blockedUntil: String? = null,
    val serverStatus: String? = null,
    val conflictReason: String? = null,
)

data class SyncSummary(val prospectsSynced: Int, val reservationsSynced: Int, val conflicts: Int)

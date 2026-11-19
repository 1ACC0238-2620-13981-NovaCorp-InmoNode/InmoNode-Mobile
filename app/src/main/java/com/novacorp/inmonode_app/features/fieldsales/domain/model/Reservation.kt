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
    val terms: NegotiationTerms = NegotiationTerms(),
) {
    /** The server gave the lot to another operation and the prospect still waits for another lot (US-12). */
    val awaitsReassignment: Boolean get() = status == ReservationStatus.CONFLICT && conflictReason != REASSIGNED

    companion object {
        /** Conflict reason left on a conflicting separation once its prospect moved to another lot. */
        const val REASSIGNED = "REASSIGNED"
        /** Conflict reasons sent by POST /field-sync. */
        const val LOT_UNAVAILABLE = "LOT_UNAVAILABLE"
        const val LOT_NOT_FOUND = "LOT_NOT_FOUND"
    }
}

data class SyncSummary(val prospectsSynced: Int, val reservationsSynced: Int, val conflicts: Int)

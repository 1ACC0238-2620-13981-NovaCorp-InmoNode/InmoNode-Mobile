package com.novacorp.inmonode_app.features.vouchers.domain

/**
 * Photo of the buyer's identity document captured with the separation (US-59), already compressed (US-08) and
 * waiting to be sent after the voucher in the next sync (US-11).
 */
data class IdentityDocument(val id: String, val reservationId: String, val imagePath: String,
    val status: IdentityDocumentStatus, val capturedAt: String, val uploaded: Boolean = false,
    val errorMessage: String? = null) {
    /** It can still be retaken: no part of it reached the server yet. */
    val replaceable: Boolean get() = status == IdentityDocumentStatus.READY_TO_SYNC && !uploaded
}

enum class IdentityDocumentStatus { READY_TO_SYNC, SYNCED }

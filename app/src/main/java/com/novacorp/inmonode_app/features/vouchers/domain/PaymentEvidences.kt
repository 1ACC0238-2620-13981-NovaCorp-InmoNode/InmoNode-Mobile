package com.novacorp.inmonode_app.features.vouchers.domain

import java.math.BigDecimal

data class PaymentEvidences(val transactionId: String, val channel: String, val status: String,
    val initialAmount: BigDecimal, val currency: String, val waitingUntil: String?,
    val evidences: List<PaymentEvidence>, val isCached: Boolean = false, val fetchedAt: String? = null)

data class PaymentEvidence(val id: Long, val reference: String, val status: String,
    val amount: BigDecimal, val currency: String, val operationDate: String,
    val operationCode: String, val late: Boolean, val submittedAt: String,
    val reviewedAt: String?, val rejectionReason: String?, val downloadUrl: String?,
    val downloadExpiresAt: String?)

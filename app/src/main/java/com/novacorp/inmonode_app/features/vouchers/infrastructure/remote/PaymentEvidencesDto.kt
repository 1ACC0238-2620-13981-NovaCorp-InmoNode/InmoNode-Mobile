package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

import java.math.BigDecimal

data class PaymentEvidencesDto(val transactionId: String, val channel: String, val status: String,
    val initialAmount: BigDecimal, val currency: String, val waitingUntil: String?, val evidences: List<PaymentEvidenceDto>)
data class PaymentEvidenceDto(val id: Long, val reference: String, val status: String,
    val amount: BigDecimal, val currency: String, val operationDate: String, val operationCode: String,
    val late: Boolean, val submittedAt: String, val reviewedAt: String?, val rejectionReason: String?,
    val downloadUrl: String?, val downloadExpiresAt: String?)

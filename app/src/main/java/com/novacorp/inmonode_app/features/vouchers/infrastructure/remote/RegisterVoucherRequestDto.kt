package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

import java.math.BigDecimal

data class RegisterVoucherRequestDto(val voucherId: String, val reservationId: String,
    val contentType: String, val sizeBytes: Long, val amount: BigDecimal, val currency: String,
    val operationDate: String, val operationCode: String, val ocrConfidence: BigDecimal?,
    val manuallyCorrected: Boolean)
data class VoucherRegistrationDto(val voucherId: String, val reservationId: String,
    val status: String, val result: String, val receivedAt: String)

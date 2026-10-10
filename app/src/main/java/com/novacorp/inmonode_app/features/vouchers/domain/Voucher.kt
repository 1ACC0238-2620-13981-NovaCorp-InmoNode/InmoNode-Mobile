package com.novacorp.inmonode_app.features.vouchers.domain

data class Voucher(
    val id: String,
    val reservationId: String,
    val imagePath: String,
    val data: OcrData = OcrData(),
    val manuallyCorrected: Boolean = false,
    val status: VoucherStatus = VoucherStatus.PENDING_OCR,
    val capturedAt: String,
    val errorMessage: String? = null,
)

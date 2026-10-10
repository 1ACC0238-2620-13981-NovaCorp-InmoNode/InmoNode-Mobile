package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

data class UploadUrlRequestDto(val voucherId: String, val reservationId: String,
    val contentType: String, val sizeBytes: Long)

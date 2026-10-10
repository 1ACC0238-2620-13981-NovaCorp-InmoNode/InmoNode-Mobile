package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

data class UploadUrlResponseDto(val uploadUrl: String, val objectKey: String,
    val expiresAt: String, val headers: Map<String, String>)

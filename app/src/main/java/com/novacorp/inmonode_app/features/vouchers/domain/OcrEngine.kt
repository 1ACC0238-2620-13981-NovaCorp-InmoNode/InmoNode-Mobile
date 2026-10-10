package com.novacorp.inmonode_app.features.vouchers.domain

data class OcrReading(val data: OcrData, val requiresRecapture: Boolean)

interface OcrEngine {
    suspend fun recognize(imagePath: String): Result<OcrReading>
}

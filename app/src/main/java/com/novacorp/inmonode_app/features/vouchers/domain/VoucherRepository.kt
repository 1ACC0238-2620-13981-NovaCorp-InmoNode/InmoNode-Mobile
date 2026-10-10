package com.novacorp.inmonode_app.features.vouchers.domain

import kotlinx.coroutines.flow.Flow

interface VoucherRepository {
    fun observeVouchers(): Flow<List<Voucher>>
    suspend fun capture(reservationId: String, imagePath: String): Result<Voucher>
    suspend fun processOcr(voucherId: String): Result<Voucher>
    suspend fun review(voucherId: String, data: OcrData): Result<Voucher>
    suspend fun upload(voucherId: String): Result<Voucher>
    suspend fun getPaymentEvidences(reservationId: String): Result<PaymentEvidences>
}

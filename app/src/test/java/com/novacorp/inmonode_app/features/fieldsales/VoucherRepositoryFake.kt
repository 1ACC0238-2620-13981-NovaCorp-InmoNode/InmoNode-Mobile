package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.domain.PaymentEvidences
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import kotlinx.coroutines.flow.Flow

/** Counts voucher cleanups; the other operations are not used by the field-sales tests. */
internal class VoucherRepositoryFake(private val discardResult: Result<Unit> = Result.success(Unit)) : VoucherRepository {
    var discards = 0
    override suspend fun discardCancelled(): Result<Unit> { discards++; return discardResult }
    override fun observeVouchers(): Flow<List<Voucher>> = error("unused")
    override suspend fun capture(reservationId: String, imagePath: String): Result<Voucher> = error("unused")
    override suspend fun processOcr(voucherId: String): Result<Voucher> = error("unused")
    override suspend fun review(voucherId: String, data: OcrData): Result<Voucher> = error("unused")
    override suspend fun upload(voucherId: String): Result<Voucher> = error("unused")
    override suspend fun getPaymentEvidences(reservationId: String): Result<PaymentEvidences> = error("unused")
}

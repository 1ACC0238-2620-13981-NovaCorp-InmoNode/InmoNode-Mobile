package com.novacorp.inmonode_app.features.vouchers.application

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import javax.inject.Inject

class ReviewVoucherUseCase @Inject constructor(private val repository: VoucherRepository) {
    suspend operator fun invoke(voucherId: String, data: OcrData) = repository.review(voucherId, data)
}

package com.novacorp.inmonode_app.features.vouchers.application

import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import javax.inject.Inject

class GetPaymentEvidencesUseCase @Inject constructor(private val repository: VoucherRepository) {
    suspend operator fun invoke(reservationId: String) = repository.getPaymentEvidences(reservationId)
}

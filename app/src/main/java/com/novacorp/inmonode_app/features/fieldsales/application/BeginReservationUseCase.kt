package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import java.math.BigDecimal
import javax.inject.Inject

/** The wizard's draft is durable, but cannot synchronize until its reviewed voucher is confirmed. */
class BeginReservationUseCase @Inject constructor(private val repository: ReservationRepository) {
    suspend operator fun invoke(lotId: Long, prospectId: String, initialAmount: BigDecimal) =
        repository.createDraft(lotId, prospectId, initialAmount)
}

package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import javax.inject.Inject

/** Discarding a draft also discards its voucher; if that step fails, the next synchronization retries it. */
class CancelReservationDraftUseCase @Inject constructor(private val repository: ReservationRepository,
    private val vouchers: VoucherRepository) {
    suspend operator fun invoke(reservationId: String): Result<Unit> =
        repository.cancelDraft(reservationId).onSuccess { vouchers.discardCancelled() }
}

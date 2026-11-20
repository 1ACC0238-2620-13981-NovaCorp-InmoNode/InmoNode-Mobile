package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractDocumentRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import javax.inject.Inject

/**
 * Discarding a draft also discards its voucher and its preliminary contract PDF, which hold the buyer's data.
 * If the voucher step fails, the next synchronization retries it; the PDF lives in the cache and is replaced on reuse.
 */
class CancelReservationDraftUseCase @Inject constructor(private val repository: ReservationRepository,
    private val vouchers: VoucherRepository, private val contracts: ContractDocumentRepository) {
    suspend operator fun invoke(reservationId: String): Result<Unit> =
        repository.cancelDraft(reservationId).onSuccess {
            vouchers.discardCancelled()
            runCatching { contracts.delete(reservationId) }
        }
}

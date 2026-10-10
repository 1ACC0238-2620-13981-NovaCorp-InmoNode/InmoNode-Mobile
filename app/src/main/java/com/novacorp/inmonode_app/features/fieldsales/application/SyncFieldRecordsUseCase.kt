package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.fieldsales.domain.model.SyncSummary
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.FieldSyncRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncFieldRecordsUseCase @Inject constructor(private val fields: FieldSyncRepository,
    private val reservations: ReservationRepository, private val vouchers: VoucherRepository,
    private val auth: AuthRepository) {
    private val mutex = Mutex()
    suspend operator fun invoke(): Result<SyncSummary> = mutex.withLock {
        val owner = auth.currentUser.first()?.takeIf { it.isFieldAgent }?.id
            ?: return@withLock Result.failure(IllegalStateException("Inicia sesión como agente de campo."))
        val summary = fields.synchronize().getOrElse { return@withLock Result.failure(it) }
        if (auth.currentUser.first()?.id != owner) return@withLock sessionChanged()
        val syncedIds = reservations.observeReservations().first()
            .filter { it.status == ReservationStatus.SYNCED }.map { it.id }.toSet()
        // Reservations are acknowledged before requesting presigned URLs; conflicts never upload their voucher.
        val pending = vouchers.observeVouchers().first().filter {
            it.status == VoucherStatus.READY_TO_SYNC && it.reservationId in syncedIds
        }.sortedBy { it.capturedAt }
        for (voucher in pending) {
            if (auth.currentUser.first()?.id != owner) return@withLock sessionChanged()
            vouchers.upload(voucher.id).getOrElse { return@withLock Result.failure(it) }
        }
        Result.success(summary)
    }

    private fun sessionChanged(): Result<SyncSummary> =
        Result.failure(IllegalStateException("La sesión cambió. Tus registros siguen guardados para su agente original."))
}

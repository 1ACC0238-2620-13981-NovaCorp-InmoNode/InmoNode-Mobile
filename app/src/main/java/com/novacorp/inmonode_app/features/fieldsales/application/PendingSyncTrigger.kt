package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.core.di.ApplicationScope
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Prospect
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ProspectRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues the background sync (US-11) whenever the agent's pending records change, and once at app start for
 * records left from a previous session. The queued sync then waits for a connection by itself.
 */
@Singleton
class PendingSyncTrigger @Inject constructor(
    private val auth: AuthRepository,
    private val reservations: ReservationRepository,
    private val prospects: ProspectRepository,
    private val vouchers: VoucherRepository,
    private val background: BackgroundSync,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start(): Job = scope.launch {
        combine(auth.currentUser, reservations.observeReservations(), prospects.observeProspects(),
            vouchers.observeVouchers()) { user, rows, people, receipts ->
            user?.takeIf { it.isFieldAgent }?.let { pendingSyncKey(it.id, rows, people, receipts) }
        }
            // An error must not end the trigger for the rest of the process.
            .retry { delay(RETRY_DELAY_MS); true }
            .distinctUntilChanged()
            .collect { key -> if (key != null) background.schedule() }
    }

    private companion object {
        const val RETRY_DELAY_MS = 5_000L
    }
}

/** What a sync would send for [owner], mirroring SyncFieldRecordsUseCase; null when nothing is pending. */
internal fun pendingSyncKey(owner: Long, reservations: List<Reservation>, prospects: List<Prospect>, vouchers: List<Voucher>): String? {
    val sendable = setOf(ReservationStatus.PENDING_SYNC, ReservationStatus.FAILED)
    val uploadable = reservations.filter { it.status in sendable || it.status == ReservationStatus.SYNCED }.map { it.id }.toSet()
    val ids = reservations.filter { it.status in sendable }.map { "r:${it.id}" } +
        prospects.filter { !it.synced }.map { "p:${it.id}" } +
        // Drafts, conflicts and cancelled separations never upload their voucher.
        vouchers.filter { it.status == VoucherStatus.READY_TO_SYNC && it.reservationId in uploadable }.map { "v:${it.id}" }
    return if (ids.isEmpty()) null else "$owner|" + ids.sorted().joinToString(",")
}

package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.SyncFieldRecordsUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.FieldSyncRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole
import com.novacorp.inmonode_app.features.vouchers.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/** The summary of "Sincronizar ahora" counts what this sync sent, vouchers included (US-11). */
class SyncFieldRecordsUseCaseTest {
    private val auth = object : AuthRepository {
        override val currentUser = MutableStateFlow<User?>(User(1, "agent@example.test", UserRole.FIELD_AGENT))
        override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
        override suspend fun signOut() = Unit
    }
    private fun fields(summary: SyncSummary) = object : FieldSyncRepository {
        override suspend fun synchronize() = Result.success(summary)
    }
    private val synced = Reservation("r1", 10, "p1", BigDecimal("500"), "2026-11-19T19:00:00Z", ReservationStatus.SYNCED)
    private val reservations = object : ReservationRepository {
        override fun observeReservations() = flowOf(listOf(synced))
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal, terms: NegotiationTerms): Result<Reservation> = error("unused")
        override suspend fun confirmDraft(reservationId: String): Result<Reservation> = error("unused")
        override suspend fun cancelDraft(reservationId: String): Result<Unit> = error("unused")
        override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = error("unused")
    }
    private fun vouchers(vararg pending: Voucher) = object : VoucherRepository {
        val uploaded = mutableListOf<String>()
        override fun observeVouchers() = flowOf(pending.toList())
        override suspend fun upload(voucherId: String): Result<Voucher> {
            uploaded += voucherId
            return Result.success(pending.first { it.id == voucherId }.copy(status = VoucherStatus.SYNCED))
        }
        override suspend fun discardCancelled() = Result.success(Unit)
        override suspend fun capture(reservationId: String, imagePath: String): Result<Voucher> = error("unused")
        override suspend fun processOcr(voucherId: String): Result<Voucher> = error("unused")
        override suspend fun review(voucherId: String, data: OcrData): Result<Voucher> = error("unused")
        override suspend fun getPaymentEvidences(reservationId: String): Result<PaymentEvidences> = error("unused")
    }

    @Test fun recordsAlreadySentByTheBackgroundSyncLeaveNothingToReport() = runTest {
        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 0, 0)), reservations, vouchers(), auth)().getOrThrow()
        assertTrue(summary.sentNothing)
    }

    @Test fun aVoucherUploadedForASyncedSeparationIsCounted() = runTest {
        val voucher = Voucher("v1", "r1", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        val receipts = vouchers(voucher)
        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 0, 0)), reservations, receipts, auth)().getOrThrow()
        assertEquals(listOf("v1"), receipts.uploaded)
        assertEquals(SyncSummary(0, 0, 0, vouchersUploaded = 1), summary)
        assertFalse(summary.sentNothing)
    }
}

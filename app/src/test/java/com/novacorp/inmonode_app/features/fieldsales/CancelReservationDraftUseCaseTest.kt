package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.CancelReservationDraftUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CancelReservationDraftUseCaseTest {
    @Test
    fun discardsTheVoucherAfterCancellingTheDraft() = runTest {
        val reservations = FakeReservations(Result.success(Unit))
        val vouchers = VoucherRepositoryFake()

        val result = CancelReservationDraftUseCase(reservations, vouchers)("draft-1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("draft-1"), reservations.cancelled)
        assertEquals(1, vouchers.discards)
    }

    @Test
    fun keepsTheVoucherWhenTheDraftCannotBeCancelled() = runTest {
        val reservations = FakeReservations(Result.failure(IllegalStateException("Solo se puede cancelar un borrador local.")))
        val vouchers = VoucherRepositoryFake()

        val result = CancelReservationDraftUseCase(reservations, vouchers)("synced-1")

        assertTrue(result.isFailure)
        assertEquals(0, vouchers.discards)
    }

    @Test
    fun aFailedVoucherCleanupDoesNotUndoTheCancellation() = runTest {
        val vouchers = VoucherRepositoryFake(Result.failure(IllegalStateException("disk")))

        val result = CancelReservationDraftUseCase(FakeReservations(Result.success(Unit)), vouchers)("draft-1")

        assertTrue(result.isSuccess)
        assertEquals(1, vouchers.discards)
    }

    private class FakeReservations(private val cancelResult: Result<Unit>) : ReservationRepository {
        val cancelled = mutableListOf<String>()
        override suspend fun cancelDraft(reservationId: String): Result<Unit> { cancelled += reservationId; return cancelResult }
        override fun observeReservations(): Flow<List<Reservation>> = error("unused")
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal): Result<Reservation> = error("unused")
        override suspend fun confirmDraft(reservationId: String): Result<Reservation> = error("unused")
        override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = error("unused")
    }
}

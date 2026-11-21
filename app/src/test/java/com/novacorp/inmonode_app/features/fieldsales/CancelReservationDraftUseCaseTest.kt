package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.CancelReservationDraftUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CancelReservationDraftUseCaseTest {
    private val contracts = ContractDocumentsFake()
    private val documents = IdentityDocumentRepositoryFake()

    @Test
    fun discardsTheVoucherTheIdentityDocumentAndTheContractAfterCancellingTheDraft() = runTest {
        val reservations = FakeReservations(Result.success(Unit))
        val vouchers = VoucherRepositoryFake()

        val result = CancelReservationDraftUseCase(reservations, vouchers, documents, contracts)("draft-1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("draft-1"), reservations.cancelled)
        assertEquals(1, vouchers.discards)
        assertEquals(1, documents.discards)
        assertEquals(listOf("draft-1"), contracts.deleted)
    }

    @Test
    fun keepsTheVoucherTheIdentityDocumentAndTheContractWhenTheDraftCannotBeCancelled() = runTest {
        val reservations = FakeReservations(Result.failure(IllegalStateException("Solo se puede cancelar un borrador local.")))
        val vouchers = VoucherRepositoryFake()

        val result = CancelReservationDraftUseCase(reservations, vouchers, documents, contracts)("synced-1")

        assertTrue(result.isFailure)
        assertEquals(0, vouchers.discards)
        assertEquals(0, documents.discards)
        assertTrue(contracts.deleted.isEmpty())
    }

    @Test
    fun aFailedVoucherCleanupDoesNotUndoTheCancellation() = runTest {
        val vouchers = VoucherRepositoryFake(Result.failure(IllegalStateException("disk")))

        val result = CancelReservationDraftUseCase(FakeReservations(Result.success(Unit)), vouchers, documents, contracts)("draft-1")

        assertTrue(result.isSuccess)
        assertEquals(1, vouchers.discards)
        assertEquals("the photo is cleaned up even when the voucher cleanup fails", 1, documents.discards)
    }

    private class FakeReservations(private val cancelResult: Result<Unit>) : ReservationRepository {
        val cancelled = mutableListOf<String>()
        override suspend fun cancelDraft(reservationId: String): Result<Unit> { cancelled += reservationId; return cancelResult }
        override fun observeReservations(): Flow<List<Reservation>> = error("unused")
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal,
            terms: NegotiationTerms): Result<Reservation> = error("unused")
        override suspend fun confirmDraft(reservationId: String): Result<Reservation> = error("unused")
        override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = error("unused")
    }
}

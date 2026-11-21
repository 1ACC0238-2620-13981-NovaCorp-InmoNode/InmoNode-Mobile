package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.BeginReservationUseCase
import com.novacorp.inmonode_app.features.fieldsales.application.ResolveConflictUseCase
import com.novacorp.inmonode_app.features.fieldsales.application.ReserveLotUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.math.BigDecimal

/**
 * T88: the separation (US-05), conflict (US-12) and negotiation (US-58) use cases hand the agent's choices to the
 * local repository as they are, and give back its answer. The rules themselves run on Room in FieldPersistenceTest.
 */
class SeparationUseCasesTest {
    private val terms = NegotiationTerms(BigDecimal("1500.00"), "Cerco perimétrico", "Aprobado por gerencia")

    private class Recording(private val rejection: Throwable? = null) : ReservationRepository {
        val calls = mutableListOf<String>()
        private fun <T> answer(row: () -> T) = rejection?.let { Result.failure<T>(it) } ?: Result.success(row())
        override fun observeReservations() = flowOf(emptyList<Reservation>())
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal, terms: NegotiationTerms): Result<Reservation> {
            calls += "draft lot $lotId for $prospectId"
            return answer { Reservation("r1", lotId, prospectId, initialAmount, AT, ReservationStatus.DRAFT, terms = terms) }
        }
        override suspend fun confirmDraft(reservationId: String): Result<Reservation> {
            calls += "confirm $reservationId"
            return answer { Reservation(reservationId, 10, "p1", BigDecimal("500.00"), AT, ReservationStatus.PENDING_SYNC) }
        }
        override suspend fun cancelDraft(reservationId: String): Result<Unit> = error("unused")
        override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> {
            calls += "reassign $reservationId to lot $newLotId"
            return answer { Reservation("r2", newLotId, "p1", BigDecimal("500.00"), AT, ReservationStatus.PENDING_SYNC) }
        }
    }

    @Test fun beginningASeparationKeepsADraftWithTheNegotiatedTerms() = runTest {
        val repository = Recording()

        val draft = BeginReservationUseCase(repository)(10, "p1", BigDecimal("500.00"), terms).getOrThrow()

        assertEquals(ReservationStatus.DRAFT, draft.status)
        assertEquals(terms, draft.terms)
        assertEquals(0, BigDecimal("8500.00").compareTo(draft.terms.finalPrice(BigDecimal("10000.00"))))
        assertEquals("nothing is confirmed until the voucher is reviewed", listOf("draft lot 10 for p1"), repository.calls)
    }

    @Test fun aSeparationWithoutNegotiationKeepsTheLotPrice() = runTest {
        val draft = BeginReservationUseCase(Recording())(10, "p1", BigDecimal("500.00")).getOrThrow()

        assertEquals(NegotiationTerms(), draft.terms)
        assertEquals(0, BigDecimal("10000.00").compareTo(draft.terms.finalPrice(BigDecimal("10000.00"))))
    }

    @Test fun reservingConfirmsOnlyTheGivenDraftSoItWaitsToSync() = runTest {
        val repository = Recording()

        val separation = ReserveLotUseCase(repository)("r1").getOrThrow()

        assertEquals(ReservationStatus.PENDING_SYNC, separation.status)
        assertEquals(listOf("confirm r1"), repository.calls)
    }

    @Test fun resolvingAConflictMovesTheProspectToTheChosenLot() = runTest {
        val repository = Recording()

        val replacement = ResolveConflictUseCase(repository)("r1", 12).getOrThrow()

        assertEquals(12L, replacement.lotId)
        assertEquals("the replacement is a new separation that waits to sync", ReservationStatus.PENDING_SYNC, replacement.status)
        assertEquals(listOf("reassign r1 to lot 12"), repository.calls)
    }

    @Test fun aRejectionReachesTheAgentWithItsMessage() = runTest {
        val taken = IllegalStateException("El lote ya no está disponible. Selecciona otro lote.")
        val repository = Recording(taken)

        assertSame(taken, BeginReservationUseCase(repository)(10, "p1", BigDecimal("500.00"), terms).exceptionOrNull())
        assertSame(taken, ReserveLotUseCase(repository)("r1").exceptionOrNull())
        assertSame(taken, ResolveConflictUseCase(repository)("r1", 12).exceptionOrNull())
    }

    private companion object {
        const val AT = "2026-11-21T10:00:00Z"
    }
}

package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSync
import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSyncState
import com.novacorp.inmonode_app.features.fieldsales.application.PendingSyncTrigger
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ProspectRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class PendingSyncTriggerTest {
    private val agent = User(1, "agent@example.test", UserRole.FIELD_AGENT)
    private val user = MutableStateFlow<User?>(agent)
    private val rows = MutableStateFlow<List<Reservation>>(emptyList())
    private val people = MutableStateFlow<List<Prospect>>(emptyList())
    private val receipts = MutableStateFlow<List<Voucher>>(emptyList())
    private val background = object : BackgroundSync {
        var scheduled = 0
        override fun schedule() { scheduled++ }
        override val state: Flow<BackgroundSyncState> = emptyFlow()
    }

    private fun TestScope.startTrigger() {
        val auth = object : AuthRepository {
            override val currentUser = user
            override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
            override suspend fun signOut() = Unit
        }
        val reservations = object : ReservationRepository {
            override fun observeReservations() = rows
            override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal,
                terms: NegotiationTerms): Result<Reservation> = error("unused")
            override suspend fun confirmDraft(reservationId: String): Result<Reservation> = error("unused")
            override suspend fun cancelDraft(reservationId: String): Result<Unit> = error("unused")
            override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = error("unused")
        }
        val prospects = object : ProspectRepository {
            override fun observeProspects() = people
            override suspend fun completeMaritalStatus(prospectId: String, maritalStatus: String): Result<Prospect> = error("unused")
            override suspend fun register(document: String, fullName: String, phone: String, maritalStatus: String?): Result<Prospect> = error("unused")
        }
        PendingSyncTrigger(auth, reservations, prospects, VoucherRepositoryFake(vouchers = receipts), background, backgroundScope).start()
        runCurrent()
    }

    private fun reservation(id: String, status: ReservationStatus) =
        Reservation(id, 1, "p", BigDecimal("500"), "2026-11-18T12:00:00Z", status)
    private fun prospect(id: String, synced: Boolean) = Prospect(id, "12345678", "Prueba", "", "Soltero(a)", "2026-11-18T12:00:00Z", synced)
    private fun voucher(id: String, reservationId: String) =
        Voucher(id, reservationId, "v.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-18T12:00:00Z")

    @Test fun recordsLeftFromBeforeAreQueuedAtStart() = runTest {
        people.value = listOf(prospect("p", synced = false))
        startTrigger()
        assertEquals(1, background.scheduled)
    }

    @Test fun nothingPendingQueuesNothing() = runTest {
        rows.value = listOf(reservation("a", ReservationStatus.SYNCED), reservation("d", ReservationStatus.DRAFT))
        people.value = listOf(prospect("p", synced = true))
        startTrigger()
        assertEquals(0, background.scheduled)
    }

    @Test fun eachNewPendingRecordQueuesAnotherSync() = runTest {
        startTrigger()
        rows.value = listOf(reservation("a", ReservationStatus.PENDING_SYNC)); runCurrent()
        rows.value = rows.value + reservation("b", ReservationStatus.PENDING_SYNC); runCurrent()
        assertEquals(2, background.scheduled)
        // A finished sync empties the queue; no new run is needed for that.
        rows.value = rows.value.map { it.copy(status = ReservationStatus.SYNCED) }; runCurrent()
        assertEquals(2, background.scheduled)
    }

    @Test fun voucherOfASyncedSeparationIsPendingButOneOfADraftIsNot() = runTest {
        rows.value = listOf(reservation("s", ReservationStatus.SYNCED), reservation("d", ReservationStatus.DRAFT))
        startTrigger()
        receipts.value = listOf(voucher("vd", "d")); runCurrent()
        assertEquals(0, background.scheduled)
        receipts.value = receipts.value + voucher("vs", "s"); runCurrent()
        assertEquals(1, background.scheduled)
    }

    @Test fun withoutAFieldAgentSessionNothingIsQueued() = runTest {
        user.value = null
        people.value = listOf(prospect("p", synced = false))
        startTrigger()
        assertEquals(0, background.scheduled)
        user.value = agent; runCurrent()
        assertEquals(1, background.scheduled)
    }
}

package com.novacorp.inmonode_app.features.vouchers

import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocument
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentStatus
import com.novacorp.inmonode_app.features.vouchers.presentation.separations.IdentityDocumentState
import com.novacorp.inmonode_app.features.vouchers.presentation.separations.SeparationsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** US-59, scenario 2: a separation without the buyer's identity document shows it as pending until it is attached. */
class SeparationIdentityDocumentTest {
    private fun separation(id: String, status: ReservationStatus) =
        Reservation(id, 10, "p1", BigDecimal("500"), "2026-11-21T10:00:00Z", status)
    private fun photo(reservationId: String, status: IdentityDocumentStatus) =
        IdentityDocument("d-$reservationId", reservationId, "$reservationId.jpg", status, "2026-11-21T10:05:00Z")

    private val state = SeparationsUiState(documents = listOf(
        photo("waiting", IdentityDocumentStatus.READY_TO_SYNC), photo("sent", IdentityDocumentStatus.SYNCED)
    ).associateBy { it.reservationId })

    @Test fun aSeparationWithoutPhotoIsPendingWhetherSyncedOrNot() {
        assertEquals(IdentityDocumentState.PENDING, state.identityDocumentState(separation("skipped", ReservationStatus.PENDING_SYNC)))
        assertEquals(IdentityDocumentState.PENDING, state.identityDocumentState(separation("skipped", ReservationStatus.SYNCED)))
        assertEquals(IdentityDocumentState.PENDING, state.identityDocumentState(separation("skipped", ReservationStatus.FAILED)))
    }

    @Test fun anAttachedPhotoIsWaitingUntilTheSyncSendsIt() {
        assertEquals(IdentityDocumentState.WAITING, state.identityDocumentState(separation("waiting", ReservationStatus.SYNCED)))
        assertEquals(IdentityDocumentState.SENT, state.identityDocumentState(separation("sent", ReservationStatus.SYNCED)))
    }

    @Test fun aConflictShowsNothingSinceItsReassignmentTakesThePhotoAlong() {
        assertNull(state.identityDocumentState(separation("lost", ReservationStatus.CONFLICT)))
        assertNull(state.identityDocumentState(separation("discarded", ReservationStatus.CANCELLED)))
    }
}

package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ReservationConflictTest {
    private fun reservation(status: ReservationStatus, reason: String? = null) =
        Reservation("r", 1, "p", BigDecimal("500"), "2026-11-18T12:00:00Z", status, conflictReason = reason)

    @Test fun onlyAConflictNotYetReassignedAwaitsTheAgent() {
        assertEquals(true, reservation(ReservationStatus.CONFLICT, Reservation.LOT_UNAVAILABLE).awaitsReassignment)
        assertEquals(true, reservation(ReservationStatus.CONFLICT, Reservation.LOT_NOT_FOUND).awaitsReassignment)
        assertEquals(false, reservation(ReservationStatus.CONFLICT, Reservation.REASSIGNED).awaitsReassignment)
        for (status in ReservationStatus.entries - ReservationStatus.CONFLICT) {
            assertEquals(status.name, false, reservation(status).awaitsReassignment)
        }
    }
}

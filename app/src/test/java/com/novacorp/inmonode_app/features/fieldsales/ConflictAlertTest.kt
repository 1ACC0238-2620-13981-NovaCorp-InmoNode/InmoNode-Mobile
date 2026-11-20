package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.sync.SyncQueueUiState
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ConflictAlertTest {
    private val lot = Lot(2, 1, "N-02", LotDimensions(BigDecimal.TEN, BigDecimal.TEN, BigDecimal("100")), emptyList(),
        BigDecimal("48000"), "PEN", LotStatus.BLOCKED)
    private val project = Project(1, "Proyecto", "Lima", null, null, null,
        FinancingRules(BigDecimal("20"), BigDecimal.ZERO, 120, BigDecimal.ZERO), listOf(lot))
    private val state = SyncQueueUiState(portfolio = Portfolio(listOf(project), null, null))
    private fun conflict(lotId: Long) = Reservation("r", lotId, "p", BigDecimal("500"), "2026-11-18T12:00:00Z",
        ReservationStatus.CONFLICT, conflictReason = Reservation.LOT_UNAVAILABLE)

    @Test fun theLostLotIsNamedWithItsCodeAndTheAmountWithItsCurrency() {
        assertEquals("N-02", state.lotLabel(2))
        assertEquals("PEN 500.00", state.amountLabel(conflict(2)))
    }

    @Test fun aLotGoneFromTheCatalogStillHasALabelAndTheAmountIsKept() {
        assertEquals("#9", state.lotLabel(9))
        assertEquals("500.00", state.amountLabel(conflict(9)))
    }

    @Test fun theVoucherOfAConflictIsNotPendingUntilTheSeparationGetsANewLot() {
        val voucher = Voucher("v", "r", "v.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-18T12:00:00Z")
        assertEquals(false, state.copy(reservations = listOf(conflict(2)), vouchers = listOf(voucher)).hasPending)
        val synced = conflict(2).copy(status = ReservationStatus.SYNCED, conflictReason = null)
        assertEquals(true, state.copy(reservations = listOf(synced), vouchers = listOf(voucher)).hasPending)
    }
}

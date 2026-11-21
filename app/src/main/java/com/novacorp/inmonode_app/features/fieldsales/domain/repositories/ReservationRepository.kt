package com.novacorp.inmonode_app.features.fieldsales.domain.repositories

import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface ReservationRepository {
    fun observeReservations(): Flow<List<Reservation>>

    /** Rejects [terms] whose discount is outside the lot's valid range and an initial amount above the final price. */
    suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal,
        terms: NegotiationTerms = NegotiationTerms()): Result<Reservation>
    suspend fun confirmDraft(reservationId: String): Result<Reservation>
    suspend fun cancelDraft(reservationId: String): Result<Unit>
    suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation>
}

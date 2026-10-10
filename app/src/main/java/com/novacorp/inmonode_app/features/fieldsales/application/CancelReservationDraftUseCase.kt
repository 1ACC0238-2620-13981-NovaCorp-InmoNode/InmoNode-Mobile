package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import javax.inject.Inject

class CancelReservationDraftUseCase @Inject constructor(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String) = repository.cancelDraft(reservationId)
}

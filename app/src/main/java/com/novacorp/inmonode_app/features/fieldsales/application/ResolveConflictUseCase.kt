package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import javax.inject.Inject

class ResolveConflictUseCase @Inject constructor(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String, newLotId: Long) = repository.reassign(reservationId, newLotId)
}

package com.novacorp.inmonode_app.features.vouchers.application

import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentRepository
import javax.inject.Inject

class CaptureIdentityDocumentUseCase @Inject constructor(private val repository: IdentityDocumentRepository) {
    suspend operator fun invoke(reservationId: String, imagePath: String) = repository.capture(reservationId, imagePath)
}

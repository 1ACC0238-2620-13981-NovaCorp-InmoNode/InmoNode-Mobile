package com.novacorp.inmonode_app.features.vouchers.infrastructure.local

import com.google.gson.Gson
import com.novacorp.inmonode_app.features.vouchers.domain.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.PaymentEvidencesDto
import java.time.Instant

internal val voucherGson = Gson()
internal fun VoucherEntity.ocrData(): OcrData = voucherGson.fromJson(dataJson, OcrData::class.java)
internal fun VoucherEntity.toDomain() = Voucher(id, reservationId, imagePath, ocrData(),
    manuallyCorrected, VoucherStatus.valueOf(status), capturedAt, errorMessage)
internal fun PaymentEvidencesDto.toDomain(fetchedAt: String, isCached: Boolean = false): PaymentEvidences {
    val now = Instant.now()
    return PaymentEvidences(transactionId, channel, status, initialAmount, currency, waitingUntil, evidences.map { evidence ->
        val usableUrl = evidence.downloadExpiresAt?.let { Instant.parse(it).isAfter(now) } == true && evidence.status == "APPROVED"
        PaymentEvidence(evidence.id, evidence.reference, evidence.status, evidence.amount, evidence.currency,
            evidence.operationDate, evidence.operationCode, evidence.late, evidence.submittedAt, evidence.reviewedAt,
            evidence.rejectionReason, evidence.downloadUrl.takeIf { usableUrl }, evidence.downloadExpiresAt)
    }, isCached, fetchedAt)
}

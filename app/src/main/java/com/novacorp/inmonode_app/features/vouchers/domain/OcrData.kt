package com.novacorp.inmonode_app.features.vouchers.domain

import java.math.BigDecimal
import java.time.LocalDate

data class OcrData(
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val operationDate: String? = null,
    val operationCode: String? = null,
    /** Null when the engine does not provide a calibrated confidence score. */
    val confidence: BigDecimal? = null,
) {
    fun isValid(today: LocalDate = LocalDate.now()): Boolean {
        val date = operationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return amount != null && amount > BigDecimal.ZERO &&
            currency != null && runCatching { java.util.Currency.getInstance(currency) }.isSuccess &&
            date != null && !date.isAfter(today) && !operationCode.isNullOrBlank() &&
            operationCode.length <= 50 && (confidence == null || confidence in BigDecimal.ZERO..BigDecimal.ONE)
    }
}

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
    /** Each field that still needs a fix, so the review screen can point at it (US-10). */
    fun fieldErrors(today: LocalDate = LocalDate.now()): Set<OcrFieldError> = buildSet {
        when {
            amount == null || amount <= BigDecimal.ZERO -> add(OcrFieldError.AMOUNT)
            amount.stripTrailingZeros().scale() > 2 -> add(OcrFieldError.AMOUNT_DECIMALS)
        }
        if (currency == null || runCatching { java.util.Currency.getInstance(currency) }.isFailure) add(OcrFieldError.CURRENCY)
        val date = operationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        when {
            date == null -> add(OcrFieldError.DATE_FORMAT)
            date.isAfter(today) -> add(OcrFieldError.DATE_FUTURE)
        }
        when {
            operationCode.isNullOrBlank() -> add(OcrFieldError.CODE_MISSING)
            operationCode.length > 50 -> add(OcrFieldError.CODE_TOO_LONG)
        }
    }

    fun isValid(today: LocalDate = LocalDate.now()): Boolean =
        fieldErrors(today).isEmpty() && (confidence == null || confidence in BigDecimal.ZERO..BigDecimal.ONE)

    /** True when the agent changed what the engine read; back-office is told through the manual flag (US-10). */
    fun differsFrom(reading: OcrData): Boolean {
        // 500 and 500.00 are the same amount.
        val amountChanged = if (amount == null || reading.amount == null) amount != reading.amount else amount.compareTo(reading.amount) != 0
        return amountChanged || reading.currency != currency ||
            reading.operationDate != operationDate || reading.operationCode != operationCode
    }
}

enum class OcrFieldError { AMOUNT, AMOUNT_DECIMALS, CURRENCY, DATE_FORMAT, DATE_FUTURE, CODE_MISSING, CODE_TOO_LONG }

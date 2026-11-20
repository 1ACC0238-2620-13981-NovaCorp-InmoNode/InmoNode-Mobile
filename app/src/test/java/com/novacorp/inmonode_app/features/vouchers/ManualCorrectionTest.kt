package com.novacorp.inmonode_app.features.vouchers

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.domain.OcrFieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** US-10: the agent fixes what the OCR misread, and back-office learns it was edited. */
class ManualCorrectionTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val reading = OcrData(BigDecimal("580.00"), "PEN", "2026-11-19", "T60TEST001", BigDecimal("0.82"))

    @Test fun aFixedDigitIsAChangeButTheSameAmountWrittenDifferentlyIsNot() {
        assertTrue(reading.copy(amount = BigDecimal("500.00")).differsFrom(reading))
        assertFalse(reading.copy(amount = BigDecimal("580")).differsFrom(reading))
        assertFalse(reading.differsFrom(reading))
    }

    @Test fun anyEditedFieldOrAFieldTheOcrMissedCountsAsACorrection() {
        assertTrue(reading.copy(operationCode = "T60TEST002").differsFrom(reading))
        assertTrue(reading.copy(operationDate = "2026-11-18").differsFrom(reading))
        assertTrue(reading.copy(currency = "USD").differsFrom(reading))
        assertTrue(reading.differsFrom(reading.copy(amount = null)))
    }

    @Test fun aCompleteReadingHasNoFieldToFix() {
        assertEquals(emptySet<OcrFieldError>(), reading.fieldErrors(today))
        assertTrue(reading.isValid(today))
    }

    @Test fun eachWrongFieldIsNamed() {
        val wrong = OcrData(BigDecimal("-1"), "SOLES", "19/11/2026", " ", null)
        assertEquals(setOf(OcrFieldError.AMOUNT, OcrFieldError.CURRENCY, OcrFieldError.DATE_FORMAT, OcrFieldError.CODE_MISSING),
            wrong.fieldErrors(today))
        assertEquals(setOf(OcrFieldError.AMOUNT, OcrFieldError.CURRENCY, OcrFieldError.DATE_FORMAT, OcrFieldError.CODE_MISSING),
            OcrData().fieldErrors(today))
    }

    @Test fun amountsKeepTwoDecimalsAndDatesCannotBeLaterThanToday() {
        assertEquals(setOf(OcrFieldError.AMOUNT_DECIMALS), reading.copy(amount = BigDecimal("500.005")).fieldErrors(today))
        assertTrue(reading.copy(amount = BigDecimal("500.500")).isValid(today))
        assertEquals(setOf(OcrFieldError.DATE_FUTURE), reading.copy(operationDate = "2026-11-20").fieldErrors(today))
        assertEquals(setOf(OcrFieldError.DATE_FORMAT), reading.copy(operationDate = "2026-02-30").fieldErrors(today))
        assertEquals(setOf(OcrFieldError.CODE_TOO_LONG), reading.copy(operationCode = "1".repeat(51)).fieldErrors(today))
        assertFalse(reading.copy(amount = BigDecimal("500.005")).isValid(today))
    }
}

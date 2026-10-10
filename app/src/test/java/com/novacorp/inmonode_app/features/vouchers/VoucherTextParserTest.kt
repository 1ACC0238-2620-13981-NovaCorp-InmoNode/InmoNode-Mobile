package com.novacorp.inmonode_app.features.vouchers

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr.VoucherTextParser
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class VoucherTextParserTest {
    @Test fun extractsPeruvianBankDataWithoutInventingFields() {
        val result = VoucherTextParser.parse("Monto: S/. 1,250.50\nFecha: 09/10/2026\nNúmero de operación: 00123456", BigDecimal("0.91"))
        assertEquals(BigDecimal("1250.50"), result.amount)
        assertEquals("PEN", result.currency)
        assertEquals("2026-10-09", result.operationDate)
        assertEquals("00123456", result.operationCode)
        assertEquals(BigDecimal("0.91"), result.confidence)
        assertTrue(result.isValid(LocalDate.of(2026, 10, 9)))
    }

    @Test fun handlesCommaDecimalAndPreservesLeadingZeros() {
        val result = VoucherTextParser.parse("Importe: PEN 1.250,50\nOperación: 00000017\n2026-10-08", null)
        assertEquals(BigDecimal("1250.50"), result.amount)
        assertEquals("00000017", result.operationCode)
        assertNull(result.confidence)
    }

    @Test fun missingDateOrUnlabelledNumberRequiresManualReview() {
        val result = VoucherTextParser.parse("00000123\n09/10/2026", null)
        assertNull(result.amount)
        assertNull(result.currency)
        assertNull(result.operationCode)
        assertFalse(result.isValid(LocalDate.of(2026, 10, 9)))
    }

    @Test fun rejectsImpossibleDatesFutureDatesAndInvalidConfidence() {
        assertNull(VoucherTextParser.parse("Monto: PEN 100\n31/02/2026\nOperación: 123456", null).operationDate)
        val valid = OcrData(BigDecimal("100"), "PEN", "2026-10-09", "123456", BigDecimal("0.8"))
        assertFalse(valid.copy(operationDate = "2026-10-10").isValid(LocalDate.of(2026, 10, 9)))
        assertFalse(valid.copy(amount = BigDecimal.ZERO).isValid(LocalDate.of(2026, 10, 9)))
        assertFalse(valid.copy(confidence = BigDecimal("1.1")).isValid(LocalDate.of(2026, 10, 9)))
    }

    @Test fun foreignExchangeReferenceCannotReplaceTheCurrencyOfTheAmount() {
        val result = VoucherTextParser.parse("Monto: S/. 100.00\nReferencia USD 25\nOperación: 00012345\n09/10/2026", null)
        assertEquals("PEN", result.currency)
        assertEquals(BigDecimal("100.00"), result.amount)
    }

    @Test fun conflictingAmountsAreLeftForManualReview() {
        val result = VoucherTextParser.parse("Monto: PEN 100.00\nTotal: PEN 110.00\nOperación: 00012345\n09/10/2026", null)
        assertNull(result.amount)
        assertFalse(result.isValid(LocalDate.of(2026, 10, 9)))
    }
}

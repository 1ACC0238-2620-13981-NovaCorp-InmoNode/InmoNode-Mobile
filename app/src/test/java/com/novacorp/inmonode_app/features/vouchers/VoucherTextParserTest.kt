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

    @Test fun aHeadingIsNoOperationCodeAndShortLabelsAreRead() {
        assertEquals("00123456", VoucherTextParser.parse("Operación exitosa\nNúmero de operación  00123456", null).operationCode)
        assertEquals("03817425", VoucherTextParser.parse("Nro. de operación  03817425", null).operationCode)
        assertEquals("004512", VoucherTextParser.parse("NRO OPERACION: 004512", null).operationCode)
        assertEquals("55120983", VoucherTextParser.parse("N° de operación: 55120983", null).operationCode)
        assertEquals("PL-88213", VoucherTextParser.parse("Código de operación  PL-88213", null).operationCode)
    }

    @Test fun transferLabelsAndAThousandsGroupSplitByTheOcrAreRead() {
        assertEquals(BigDecimal("1250.50"), VoucherTextParser.parse("Monto transferido  S/ 1,250.50", null).amount)
        assertEquals(BigDecimal("3000.00"), VoucherTextParser.parse("Monto enviado  S/ 3,000.00", null).amount)
        val ticket = VoucherTextParser.parse("IMPORTE: S/ 2, 500.00", null)
        assertEquals(BigDecimal("2500.00"), ticket.amount)
        assertEquals("PEN", ticket.currency)
    }

    @Test fun withoutALabelOnlyASingleAmountWithItsCurrencyIsTaken() {
        val wallet = VoucherTextParser.parse("¡Pago exitoso!\nS/ 50\nInmobiliaria Prueba SAC", null)
        assertEquals(BigDecimal("50"), wallet.amount)
        assertEquals("PEN", wallet.currency)
        assertNull(VoucherTextParser.parse("S/ 50\nComisión S/ 1.50", null).amount)
        assertNull(VoucherTextParser.parse("Cuenta 191-0488\n15.11.2026", null).amount)
    }

    @Test fun datesWithTheMonthInLettersAreRead() {
        assertEquals("2026-11-19", VoucherTextParser.parse("19 nov. 2026| 02:32 p. m.", null).operationDate)
        assertEquals("2026-09-05", VoucherTextParser.parse("5 de setiembre de 2026", null).operationDate)
        assertEquals("2026-09-05", VoucherTextParser.parse("05 Sep 2026", null).operationDate)
        assertNull(VoucherTextParser.parse("31 feb. 2026", null).operationDate)
        assertEquals("2026-11-15", VoucherTextParser.parse("Fecha  15.11.2026\n1 dic 2026", null).operationDate)
    }

    @Test fun anOperationCodeNeedsADigitOnTheRowOfItsLabel() {
        assertNull(VoucherTextParser.parse("Operación exitosa\nBuscar", null).operationCode)
        assertNull(VoucherTextParser.parse("Nro. de operación\nO Buscar\n03817425", null).operationCode)
        assertNull(VoucherTextParser.parse("Fecha de operación: 2026-11-17", null).operationCode)
    }
}

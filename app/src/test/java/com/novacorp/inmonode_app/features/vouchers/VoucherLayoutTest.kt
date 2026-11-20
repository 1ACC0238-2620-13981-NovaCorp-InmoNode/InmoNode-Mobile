package com.novacorp.inmonode_app.features.vouchers

import com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr.OcrLine
import com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr.VoucherLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class VoucherLayoutTest {
    @Test fun aLabelAndItsValueReadInSeparateColumnsShareARow() {
        // The order ML Kit returned a two-column transfer receipt: every label first, then every value.
        val lines = listOf(
            OcrLine("Banco Andino (prueba)", 100, 100, 140),
            OcrLine("Operación exitosa", 100, 200, 250),
            OcrLine("Monto transferido", 100, 300, 340),
            OcrLine("Fecha y hora", 100, 400, 440),
            OcrLine("Número de operación", 100, 500, 540),
            OcrLine("S/ 1,250.50", 700, 305, 345),
            OcrLine("19/11/2026 14:32", 650, 398, 438),
            OcrLine("00123456", 720, 502, 542),
        )
        assertEquals(
            "Banco Andino (prueba)\nOperación exitosa\nMonto transferido  S/ 1,250.50\n" +
                "Fecha y hora  19/11/2026 14:32\nNúmero de operación  00123456",
            VoucherLayout.rows(lines),
        )
    }

    @Test fun aSlightlyTiltedRowStaysTogetherAndBlankLinesAreDropped() {
        val lines = listOf(OcrLine("00123456", 720, 518, 558), OcrLine(" ", 0, 0, 10), OcrLine("Nro. de operación", 100, 500, 540))
        assertEquals("Nro. de operación  00123456", VoucherLayout.rows(lines))
    }

    @Test fun linesOnDifferentRowsKeepTheirOrderFromTopToBottom() {
        val lines = listOf(OcrLine("Fecha: 18/11/2026", 90, 400, 440), OcrLine("Importe: S/. 500.00", 90, 300, 340))
        assertEquals("Importe: S/. 500.00\nFecha: 18/11/2026", VoucherLayout.rows(lines))
    }
}

package com.novacorp.inmonode_app.features.vouchers

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.domain.OcrFieldError
import com.novacorp.inmonode_app.features.vouchers.presentation.ocrreview.OcrReviewUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class OcrReviewStateTest {
    private val reading = OcrData(BigDecimal("580.00"), "PEN", "2026-11-19", "T60TEST001", BigDecimal("0.82"))

    @Test fun whatTheAgentTypesReplacesTheReadingAndKeepsTheEngineConfidence() {
        val typed = OcrReviewUiState(amount = " 500,50 ", currency = "pen ", date = "2026-11-18 ", code = " T60TEST002").typed(reading)
        assertEquals(OcrData(BigDecimal("500.50"), "PEN", "2026-11-18", "T60TEST002", BigDecimal("0.82")), typed)
    }

    @Test fun anAmountThatIsNotANumberIsLeftEmpty() {
        assertNull(OcrReviewUiState(amount = "1,500.00").typed(reading).amount)
        assertNull(OcrReviewUiState(amount = "S/ 500").typed(reading).amount)
    }

    @Test fun fieldsAreFlaggedOnlyOnceChecked() {
        val wrong = OcrReviewUiState(amount = "abc", currency = "PEN", date = "19/11/2026", code = "T60TEST001")
        assertEquals(emptySet<OcrFieldError>(), wrong.fieldErrors)
        assertEquals(setOf(OcrFieldError.AMOUNT, OcrFieldError.DATE_FORMAT), wrong.copy(checked = true).fieldErrors)
    }
}

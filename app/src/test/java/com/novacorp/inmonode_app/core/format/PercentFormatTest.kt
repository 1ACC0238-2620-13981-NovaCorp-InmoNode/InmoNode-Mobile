package com.novacorp.inmonode_app.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PercentFormatTest {

    @Test
    fun ratesDropTrailingZerosAndKeepTheSpacedSign() {
        assertEquals("12.5 %", formatPercent(BigDecimal("12.5000")))
        assertEquals("20 %", formatPercent(BigDecimal("20.00")))
        assertEquals("0 %", formatPercent(BigDecimal("0.00")))
    }
}

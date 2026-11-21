package com.novacorp.inmonode_app.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {

    @Test
    fun solesUseTheSymbolAndThousandsSeparators() {
        assertEquals("S/ 44,300.00", formatMoney("PEN", BigDecimal("44300")))
        assertEquals("S/ 1,250,000.50", formatMoney("PEN", BigDecimal("1250000.5")))
        assertEquals("S/ 0.00", formatMoney("PEN", BigDecimal.ZERO))
        assertEquals("S/ 999.99", formatMoney("pen", BigDecimal("999.99")))
    }

    @Test
    fun dollarsAndOtherCurrencies() {
        assertEquals("US$ 1,200.00", formatMoney("USD", BigDecimal("1200")))
        assertEquals("EUR 15.00", formatMoney("EUR", BigDecimal("15")))
    }

    @Test
    fun amountsAreCutToTwoDecimalsNeverRoundedUp() {
        assertEquals("S/ 1,136.57", formatMoney("PEN", BigDecimal("1136.5799")))
        assertEquals("44,299.99", formatAmount(BigDecimal("44299.999")))
    }
}

package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class NegotiationTermsTest {
    private val price = BigDecimal("45000.00")

    @Test
    fun discountLowersTheFinalPrice() {
        assertEquals(BigDecimal("42000.00"), NegotiationTerms(discount = BigDecimal("3000")).finalPrice(price))
    }

    @Test
    fun withoutDiscountTheFinalPriceIsTheLotPrice() {
        assertEquals(price, NegotiationTerms(bonus = "Cerco perimétrico").finalPrice(price))
    }

    @Test
    fun acceptsDiscountsFromZeroToJustBelowThePrice() {
        assertNull(NegotiationTerms(discount = BigDecimal.ZERO).validationError(price, "PEN"))
        assertNull(NegotiationTerms(discount = BigDecimal("44999.99")).validationError(price, "PEN"))
    }

    @Test
    fun rejectsNegativeDiscountNamingTheValidRange() {
        assertEquals("El descuento debe estar entre PEN 0.00 y PEN 44999.99.",
            NegotiationTerms(discount = BigDecimal("-1")).validationError(price, "PEN"))
    }

    @Test
    fun rejectsDiscountEqualToOrAboveThePrice() {
        val expected = "El descuento debe estar entre PEN 0.00 y PEN 44999.99."
        assertEquals(expected, NegotiationTerms(discount = BigDecimal("45000")).validationError(price, "PEN"))
        assertEquals(expected, NegotiationTerms(discount = BigDecimal("50000")).validationError(price, "PEN"))
    }

    @Test
    fun rejectsMoreThanTwoDecimals() {
        assertEquals("El descuento debe tener como máximo dos decimales.",
            NegotiationTerms(discount = BigDecimal("10.005")).validationError(price, "PEN"))
    }

    @Test
    fun rejectsTooLongTexts() {
        assertEquals("El bono debe tener como máximo 150 caracteres.",
            NegotiationTerms(bonus = "b".repeat(151)).validationError(price, "PEN"))
        assertEquals("Las observaciones deben tener como máximo 500 caracteres.",
            NegotiationTerms(notes = "n".repeat(501)).validationError(price, "PEN"))
    }

    @Test
    fun blankWizardFieldsAreNotAgreedTerms() {
        assertEquals(NegotiationTerms(), NegotiationTerms.of(null, "  ", ""))
        assertEquals(NegotiationTerms(bonus = "Cerco", notes = "Pago en 2 partes"),
            NegotiationTerms.of(null, " Cerco ", "Pago en 2 partes\n"))
    }
}

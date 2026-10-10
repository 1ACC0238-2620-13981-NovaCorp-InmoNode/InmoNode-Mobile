package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.SimulateFinancingUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.FinancingRules
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FinancingSimulationTest {
    private val simulate = SimulateFinancingUseCase()
    private fun rules(rate: String = "12.5", minimum: String = "20") =
        FinancingRules(BigDecimal(minimum), BigDecimal(rate), 360, BigDecimal("1"))

    @Test fun zeroInterestLastPaymentAbsorbsRounding() {
        val answer = simulate(BigDecimal("100.00"), BigDecimal("20.00"), 3, rules("0"), LocalDate.of(2026, 1, 31)).getOrThrow()
        assertEquals(BigDecimal("26.67"), answer.monthlyPayment)
        assertEquals(BigDecimal("26.66"), answer.installments.last().payment)
        assertEquals(BigDecimal("0.00"), answer.installments.last().balance)
        assertEquals(BigDecimal("80.00"), answer.installments.fold(BigDecimal.ZERO) { total, item -> total + item.principal })
        assertEquals(LocalDate.of(2026, 2, 28), answer.installments.first().dueDate)
        assertEquals(BigDecimal("0.00"), answer.totalInterest)
    }

    @Test fun usesEffectiveAnnualRateRatherThanDividingByTwelve() {
        val answer = simulate(BigDecimal("10000"), BigDecimal("2000"), 12, rules("12.68250301319697")).getOrThrow()
        // This effective annual rate corresponds to approximately 1% monthly.
        assertEquals(BigDecimal("710.79"), answer.monthlyPayment)
        assertEquals(BigDecimal("8000.00"), answer.installments.fold(BigDecimal.ZERO) { total, item -> total + item.principal })
        assertEquals(BigDecimal("0.00"), answer.installments.last().balance)
        assertTrue(answer.installments.all { it.principal.signum() > 0 && it.balance.signum() >= 0 })
    }

    @Test fun minimumIsRoundedUpAndProjectTermIsEnforced() {
        assertTrue(simulate(BigDecimal("100.01"), BigDecimal("20.00"), 12, rules()).isFailure)
        assertTrue(simulate(BigDecimal("100.01"), BigDecimal("20.01"), 12, rules()).isSuccess)
        assertTrue(simulate(BigDecimal("100"), BigDecimal("20"), 361, rules()).isFailure)
        assertTrue(simulate(BigDecimal("100"), BigDecimal("100"), 12, rules()).isFailure)
        assertTrue(simulate(BigDecimal("100"), BigDecimal.ZERO, 12, rules("0", "0")).isFailure)
    }

    @Test fun financialAmountsRetainCentPrecisionAcrossRatesAndTerms() {
        for (rate in listOf("0", "1.5", "12.5", "100")) {
            for (months in listOf(1, 12, 120, 360)) {
                val answer = simulate(BigDecimal("123456.78"), BigDecimal("30000.01"), months, rules(rate)).getOrThrow()
                assertEquals(BigDecimal("93456.77"), answer.installments.fold(BigDecimal.ZERO) { total, item -> total + item.principal })
                assertEquals(BigDecimal("0.00"), answer.installments.last().balance)
            }
        }
    }
}

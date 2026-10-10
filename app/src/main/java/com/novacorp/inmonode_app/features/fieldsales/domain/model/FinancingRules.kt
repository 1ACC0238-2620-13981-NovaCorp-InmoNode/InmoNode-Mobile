package com.novacorp.inmonode_app.features.fieldsales.domain.model

import java.math.BigDecimal

/** Rates are percentages, as in the backend: 12.5 means 12.5%. */
data class FinancingRules(
    val minDownPaymentPercentage: BigDecimal,
    val annualInterestRate: BigDecimal,
    val maxTermMonths: Int,
    val lateFeeRate: BigDecimal,
) {
    init {
        require(maxTermMonths in 1..360)
        require(listOf(minDownPaymentPercentage, annualInterestRate, lateFeeRate).all {
            it >= BigDecimal.ZERO && it <= BigDecimal("100")
        })
    }
}
